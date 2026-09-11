import React from 'react';
import { Radio, Mic, Volume2, Cpu, ShieldCheck } from 'lucide-react';
import { DiagnosticsState } from '../types';

interface AudioDiagnosticsProps {
  diagnostics: DiagnosticsState;
  amplitude: number;
}

export const AudioDiagnostics: React.FC<AudioDiagnosticsProps> = ({ diagnostics, amplitude }) => {
  return (
    <div className="bg-white/5 border border-white/10 rounded-2xl p-4 backdrop-blur-xl">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-white/90 flex items-center gap-2">
          <Radio className="w-4 h-4 text-sky-400" />
          Live Audio & Duplex Telemetry
        </h3>
        <span className="text-[11px] font-mono text-white/60">
          Target User: <strong className="text-white">Piyush</strong>
        </span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3 rounded-xl bg-black/30 border border-white/5">
          <div className="flex items-center gap-2 text-xs text-white/60 mb-1">
            <Mic className="w-3.5 h-3.5 text-purple-400" />
            Mic Input (PCM16)
          </div>
          <div className="text-sm font-mono font-medium text-white">16 kHz @ 1 Ch</div>
          {/* Visualizer bar */}
          <div className="w-full bg-white/10 h-1.5 rounded-full mt-2 overflow-hidden">
            <div
              className="bg-purple-500 h-full rounded-full transition-all duration-75"
              style={{ width: `${Math.min(100, amplitude * 250)}%` }}
            />
          </div>
        </div>

        <div className="p-3 rounded-xl bg-black/30 border border-white/5">
          <div className="flex items-center gap-2 text-xs text-white/60 mb-1">
            <Volume2 className="w-3.5 h-3.5 text-emerald-400" />
            Audio Output
          </div>
          <div className="text-sm font-mono font-medium text-white">24 kHz (Aoede)</div>
          <div className="text-[10px] text-emerald-400/80 mt-1">Full Duplex / Barge-in OK</div>
        </div>

        <div className="p-3 rounded-xl bg-black/30 border border-white/5">
          <div className="flex items-center gap-2 text-xs text-white/60 mb-1">
            <Cpu className="w-3.5 h-3.5 text-amber-400" />
            Live Model
          </div>
          <div className="text-xs font-mono font-medium text-white truncate">{diagnostics.model}</div>
          <div className="text-[10px] text-amber-400/80 mt-1">Bidi WebSocket</div>
        </div>

        <div className="p-3 rounded-xl bg-black/30 border border-white/5">
          <div className="flex items-center gap-2 text-xs text-white/60 mb-1">
            <ShieldCheck className="w-3.5 h-3.5 text-blue-400" />
            Safety Protocol
          </div>
          <div className="text-xs font-medium text-white">Confirmation Guard</div>
          <div className="text-[10px] text-blue-400/80 mt-1">Voice confirmation req.</div>
        </div>
      </div>
    </div>
  );
};
