import React, { useState } from 'react';
import { Sparkles, X, Send, Bot, CheckCircle2, ShieldCheck, ArrowRight } from 'lucide-react';
import { api } from '../../services/api';

interface AssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
  onStageCommand?: (cmd: any) => void;
}

export const AssistantModal: React.FC<AssistantModalProps> = ({ isOpen, onClose, onStageCommand }) => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversation, setConversation] = useState<Array<{ sender: 'user' | 'assistant'; text: string; data?: any }>>([
    {
      sender: 'assistant',
      text: 'Hello, Operator. I am the OpenBAS Grounded Operations Assistant. I analyze live telemetry, equipment relationships, and multi-agent findings to answer diagnostic questions. How can I assist you with building operations today?'
    }
  ]);

  if (!isOpen) return null;

  const sampleQuestions = [
    'Why is Floor 3 overheating?',
    'Which AHU is consuming the most energy?',
    'Show equipment with abnormal operating behavior.',
    'What is the probable cause of the supply air temperature spike?'
  ];

  const handleSend = async (textToSend?: string) => {
    const q = textToSend || query;
    if (!q.trim()) return;

    setConversation((prev) => [...prev, { sender: 'user', text: q }]);
    if (!textToSend) setQuery('');
    setLoading(true);

    try {
      const res = await api.queryAssistant(q);
      setConversation((prev) => [...prev, {
        sender: 'assistant',
        text: res.answer,
        data: res
      }]);
    } catch {
      setConversation((prev) => [...prev, {
        sender: 'assistant',
        text: 'I could not reach the AI service. Please verify python ai-service is running on port 8000.'
      }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl h-[650px] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center text-white">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white">Natural Language Operations Assistant</h2>
              <p className="text-[10px] text-slate-400 font-mono">Grounded in building telemetry & equipment graphs</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded text-slate-400 hover:text-white hover:bg-slate-800 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Message Thread */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {conversation.map((msg, idx) => (
            <div key={idx} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[85%] rounded-xl p-3.5 text-xs leading-relaxed ${
                msg.sender === 'user'
                  ? 'bg-cyan-600 text-white font-medium'
                  : 'bg-slate-950 border border-slate-800 text-slate-200 space-y-2'
              }`}>
                <div>{msg.text}</div>

                {/* Evidence points if present */}
                {msg.data?.evidence_points && (
                  <div className="pt-2 border-t border-slate-800/80 space-y-1">
                    <div className="text-[10px] uppercase font-mono text-slate-400 font-semibold">Grounded Evidence:</div>
                    {msg.data.evidence_points.map((pt: string, pIdx: number) => (
                      <div key={pIdx} className="flex items-center gap-1.5 text-[11px] text-slate-300">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                        <span>{pt}</span>
                      </div>
                    ))}
                  </div>
                )}

                {/* Suggested actions if present */}
                {msg.data?.suggested_actions && (
                  <div className="pt-2 border-t border-slate-800/80 space-y-1.5">
                    <div className="text-[10px] uppercase font-mono text-cyan-400 font-semibold">Suggested Operational Action:</div>
                    {msg.data.suggested_actions.map((act: string, aIdx: number) => (
                      <div key={aIdx} className="p-2 rounded bg-slate-900 border border-slate-800 flex items-center justify-between text-[11px] text-cyan-200">
                        <span>{act}</span>
                        <span className="text-[10px] font-mono text-slate-400">Routes to Approval Center</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}

          {loading && (
            <div className="flex justify-start">
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-slate-400 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span>
                <span>Synthesizing building context & equipment graph...</span>
              </div>
            </div>
          )}
        </div>

        {/* Suggested Queries */}
        <div className="px-4 py-2 bg-slate-950/40 border-t border-slate-800/60 flex flex-wrap gap-1.5">
          {sampleQuestions.map((sq, idx) => (
            <button
              key={idx}
              onClick={() => handleSend(sq)}
              className="text-[10px] px-2.5 py-1 rounded-full bg-slate-800/80 hover:bg-slate-700 text-slate-300 border border-slate-700/60 transition"
            >
              {sq}
            </button>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-3 border-t border-slate-800 bg-slate-950 flex items-center gap-2">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Ask about building behavior, equipment faults, or energy metrics..."
            className="flex-1 bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
          <button
            onClick={() => handleSend()}
            disabled={!query.trim() || loading}
            className="p-2 rounded-lg bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 text-white transition"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

