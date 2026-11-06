package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.CommandDto;
import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.api.dto.ProposeCommandRequest;
import com.openbas.intelligence.application.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commands")
@Tag(name = "Commands", description = "Command lifecycle and operator approval workflows")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @Operation(summary = "Propose a new building control command (evaluated by Policy Engine and Digital Twin)")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CommandDto> proposeCommand(
            @Valid @RequestBody ProposeCommandRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String proposedBy = user != null ? user.getUsername() : "OPERATOR";
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.proposeCommand(request, proposedBy));
    }

    @GetMapping
    @Operation(summary = "List commands, optionally filtered by status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<PagedResponseDto<CommandDto>> getCommands(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commandService.getCommands(status, page, size));
    }

    @GetMapping("/pending-approval")
    @Operation(summary = "Get all commands awaiting operator approval")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<CommandDto>> getPendingApprovals() {
        return ResponseEntity.ok(commandService.getPendingApprovals());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a command by ID")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CommandDto> getCommandById(@PathVariable UUID id) {
        return ResponseEntity.ok(commandService.getCommandById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending command")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CommandDto> approveCommand(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(commandService.approveCommand(id, user.getUsername()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending command")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CommandDto> rejectCommand(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(commandService.rejectCommand(id, reason, user.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a command before execution")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CommandDto> cancelCommand(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(commandService.cancelCommand(id, user.getUsername()));
    }
}
