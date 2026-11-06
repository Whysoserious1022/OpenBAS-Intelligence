package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.dto.CommandDto;
import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.api.dto.ProposeCommandRequest;
import com.openbas.intelligence.api.exception.BusinessValidationException;
import com.openbas.intelligence.api.exception.PolicyViolationException;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.domain.command.PolicyEvaluationResult;
import com.openbas.intelligence.infrastructure.gateway.protocol.IndustrialProtocolAdapter;
import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.CommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Command lifecycle management service.
 * Handles approval workflows, state transitions, gateway execution, and command expiry.
 */
@Service
@Transactional(readOnly = true)
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private static final List<String> TERMINAL_STATUSES = List.of(
        "EXECUTED", "REJECTED", "EXPIRED", "FAILED", "CANCELLED", "VERIFICATION_FAILED");

    private final CommandRepository commandRepository;
    private final PolicyEngineService policyEngineService;
    private final DigitalTwinService digitalTwinService;
    private final IndustrialProtocolAdapter gatewayAdapter;

    public CommandService(CommandRepository commandRepository) {
        this(commandRepository, null, null, null);
    }

    @Autowired
    public CommandService(CommandRepository commandRepository,
                          @Autowired(required = false) PolicyEngineService policyEngineService,
                          @Autowired(required = false) DigitalTwinService digitalTwinService,
                          @Autowired(required = false) @Qualifier("simulatorAdapter") IndustrialProtocolAdapter gatewayAdapter) {
        this.commandRepository = commandRepository;
        this.policyEngineService = policyEngineService;
        this.digitalTwinService = digitalTwinService;
        this.gatewayAdapter = gatewayAdapter;
    }

    @Transactional
    public CommandDto proposeCommand(ProposeCommandRequest request, String proposedBy) {
        CommandEntity command = new CommandEntity();
        command.setIdempotencyKey("IDEMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        command.setTargetEquipmentId(request.targetEquipmentId());
        command.setTargetPointId(request.targetPointId());
        command.setCommandType(request.commandType());
        command.setProposedValue(request.proposedValue());
        command.setUnit(request.unit());
        command.setReason(request.reason());
        command.setExpectedBenefit(request.expectedBenefit());
        command.setPotentialRisk(request.potentialRisk());
        command.setInitiatingAgent(request.initiatingAgent() != null ? request.initiatingAgent() : "OPERATOR");
        command.setConfidence(request.confidence() != null ? request.confidence() : 0.95);
        command.setProposedBy(proposedBy);
        command.setBuildingId("BUILDING-01");
        command.setExpiresAt(Instant.now().plus(60, ChronoUnit.MINUTES));

        if (policyEngineService != null) {
            PolicyEvaluationResult policy = policyEngineService.evaluate(command);
            command.setPolicyValidationResult(policy.getStatus().name());
            if (!policy.isPassed()) {
                command.setStatus("REJECTED");
                command.setPolicyRejectionReason(policy.getReason());
                commandRepository.save(command);
                throw new PolicyViolationException(
                    "Command rejected by Deterministic Safety Policy: " + policy.getReason(),
                    policy.getRuleId());
            }
        }

        if (digitalTwinService != null) {
            DigitalTwinService.SimulationResult sim = digitalTwinService.simulateAction(command);
            command.setDigitalTwinSimulated(true);
            command.setDigitalTwinPredictedOutcome(sim.predictedOutcome());
        }

        command.setStatus("PENDING_APPROVAL");
        CommandEntity saved = commandRepository.save(command);
        log.info("Command {} proposed: target={} point={} value={} status={}",
            saved.getId(), saved.getTargetEquipmentId(), saved.getTargetPointId(), saved.getProposedValue(), saved.getStatus());
        return toDto(saved);
    }

    public PagedResponseDto<CommandDto> getCommands(String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CommandDto> result;
        if (status != null && !status.isBlank()) {
            result = commandRepository.findAllByStatusOrderByCreatedAtDesc(status, pageRequest).map(this::toDto);
        } else {
            result = commandRepository.findAll(pageRequest).map(this::toDto);
        }
        return PagedResponseDto.of(result);
    }

    public List<CommandDto> getPendingApprovals() {
        return commandRepository.findPendingApprovals().stream().map(this::toDto).toList();
    }

    public CommandDto getCommandById(UUID id) {
        return commandRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Command", id.toString()));
    }

    @Transactional
    public CommandDto approveCommand(UUID id, String approvedBy) {
        CommandEntity command = getCommandEntityForAction(id, "PENDING_APPROVAL");
        if (command.getExpiresAt() != null && command.getExpiresAt().isBefore(Instant.now())) {
            command.setStatus("EXPIRED");
            commandRepository.save(command);
            throw new BusinessValidationException("Command " + id + " has expired and cannot be approved.");
        }
        command.setStatus("APPROVED");
        command.setApprovedBy(approvedBy);
        command.setApprovedAt(Instant.now());

        // Dispatch authorized command through Industrial Gateway
        if (gatewayAdapter != null) {
            IndustrialProtocolAdapter.GatewayCommand gwCmd = new IndustrialProtocolAdapter.GatewayCommand(
                command.getId().toString(),
                command.getTargetEquipmentId(),
                command.getTargetPointId(),
                command.getProposedValue(),
                command.getUnit(),
                true // simulated/safe mode
            );
            IndustrialProtocolAdapter.CommandResult result = gatewayAdapter.submitCommand(gwCmd);
            if (result.success()) {
                command.setStatus("EXECUTED");
                command.setExecutedAt(result.executedAt());
                command.setExecutionResult(result.resultMessage());
                command.setVerifiedValue(command.getProposedValue());
                command.setVerifiedAt(result.executedAt());
            } else {
                command.setStatus("FAILED");
                command.setExecutionResult(result.resultMessage());
            }
        }

        CommandEntity saved = commandRepository.save(command);
        log.info("Command {} approved by {} -> final status={}", id, approvedBy, saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public CommandDto rejectCommand(UUID id, String reason, String rejectedBy) {
        CommandEntity command = getCommandEntityForAction(id, "PENDING_APPROVAL");
        command.setStatus("REJECTED");
        command.setRejectedBy(rejectedBy);
        command.setRejectedAt(Instant.now());
        command.setRejectionReason(reason);
        CommandEntity saved = commandRepository.save(command);
        log.info("Command {} rejected by {} reason={}", id, rejectedBy, reason);
        return toDto(saved);
    }

    @Transactional
    public CommandDto cancelCommand(UUID id, String cancelledBy) {
        CommandEntity command = commandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Command", id.toString()));
        if (TERMINAL_STATUSES.contains(command.getStatus())) {
            throw new BusinessValidationException("Cannot cancel command in terminal status: " + command.getStatus());
        }
        if ("EXECUTING".equals(command.getStatus())) {
            throw new BusinessValidationException("Cannot cancel command currently executing.");
        }
        command.setStatus("CANCELLED");
        command.setRejectedBy(cancelledBy);
        command.setRejectedAt(Instant.now());
        command.setRejectionReason("Cancelled by " + cancelledBy);
        CommandEntity saved = commandRepository.save(command);
        log.info("Command {} cancelled by {}", id, cancelledBy);
        return toDto(saved);
    }

    /**
     * Expire commands that have passed their expiry time.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireCommands() {
        List<CommandEntity> expired = commandRepository.findExpiredCommands(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(cmd -> {
                cmd.setStatus("EXPIRED");
                commandRepository.save(cmd);
                log.info("Command {} expired", cmd.getId());
            });
        }
    }

    public long countPendingApprovals() { return commandRepository.countPendingApprovals(); }

    private CommandEntity getCommandEntityForAction(UUID id, String requiredStatus) {
        CommandEntity command = commandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Command", id.toString()));
        if (!requiredStatus.equals(command.getStatus())) {
            throw new BusinessValidationException(
                "Command " + id + " is in status '" + command.getStatus() +
                "', expected '" + requiredStatus + "'");
        }
        return command;
    }

    private CommandDto toDto(CommandEntity c) {
        return new CommandDto(c.getId(), c.getIdempotencyKey(),
            c.getTargetEquipmentId(), c.getTargetDeviceId(), c.getTargetPointId(),
            c.getBuildingId(), c.getCommandType(), c.getCurrentValue(), c.getProposedValue(),
            c.getUnit(), c.getReason(), c.getExpectedBenefit(), c.getPotentialRisk(),
            c.getInitiatingAgent(), c.getConfidence(), c.getStatus(),
            c.getPolicyValidationResult(), c.getPolicyRejectionReason(),
            c.isDigitalTwinSimulated(), c.getDigitalTwinPredictedOutcome(),
            c.getRequiredApprovalLevel(), c.getApprovedBy(), c.getApprovedAt(),
            c.getRejectedBy(), c.getRejectedAt(), c.getRejectionReason(),
            c.getExecutedAt(), c.getExecutionResult(), c.getVerifiedValue(),
            c.getVerifiedAt(), c.getExpiresAt(), c.getProposedBy(),
            c.getCreatedAt(), c.getUpdatedAt());
    }
}
