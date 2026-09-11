import React from 'react';
import { MoodMetadata } from '../types';

interface MoodAuraOrbProps {
  moodConfig: MoodMetadata;
  amplitude: number; // 0.0 to 1.0
  connectionState: string;
}

export const MoodAuraOrb: React.FC<MoodAuraOrbProps> = ({
  moodConfig,
  amplitude,
  connectionState
}) => {
  const baseScale = 1.0 + amplitude * 0.45;
  const outerScale = 1.2 + amplitude * 0.7;

  return (
    <div className="relative flex items-center justify-center w-64 h-64 my-6">
      {/* Outer Atmospheric Aura Glow */}
      <div
        className="absolute rounded-full filter blur-2xl opacity-60 transition-all duration-700 ease-out"
        style={{
          width: '240px',
          height: '240px',
          transform: `scale(${outerScale})`,
          background: `radial-gradient(circle, ${moodConfig.auraColor} 0%, ${moodConfig.secondaryAura} 60%, transparent 80%)`
        }}
      />

      {/* Middle Responsive Breathing Halo */}
      <div
        className="absolute rounded-full filter blur-md opacity-75 transition-all duration-500 ease-out animate-pulse"
        style={{
          width: '180px',
          height: '180px',
          transform: `scale(${baseScale})`,
          background: `radial-gradient(circle, ${moodConfig.secondaryAura} 20%, ${moodConfig.auraColor} 80%)`,
          boxShadow: `0 0 40px ${moodConfig.auraColor}`
        }}
      />

      {/* Core Holographic Core */}
      <div
        className="relative z-10 flex flex-col items-center justify-center w-36 h-36 rounded-full border border-white/30 backdrop-blur-md shadow-2xl transition-all duration-500"
        style={{
          background: `linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(20,20,30,0.8) 100%)`,
          boxShadow: `inset 0 0 20px ${moodConfig.auraColor}, 0 0 30px ${moodConfig.secondaryAura}`
        }}
      >
        <span className="text-4xl filter drop-shadow-md select-none">{moodConfig.emoji}</span>
        <span className="text-xs font-semibold text-white/90 mt-1 uppercase tracking-wider">
          {moodConfig.mood}
        </span>
        <span className="text-[10px] text-white/70 font-mono">
          {connectionState}
        </span>
      </div>

      {/* Orbiting Particle Rings */}
      <div
        className="absolute w-52 h-52 rounded-full border border-dashed border-white/20 animate-spin"
        style={{ animationDuration: '18s' }}
      />
    </div>
  );
};
