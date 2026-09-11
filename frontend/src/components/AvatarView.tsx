import React, { useState } from 'react';
import { MyraaMood, MoodMetadata } from '../types';

interface AvatarViewProps {
  moodConfig: MoodMetadata;
  connectionState: string;
}

export const AvatarView: React.FC<AvatarViewProps> = ({ moodConfig, connectionState }) => {
  const [videoError, setVideoError] = useState(false);

  // Video path mapping based on connection state
  const getVideoSrc = () => {
    if (connectionState === 'SPEAKING') return '/assets/talking.mp4';
    if (connectionState === 'CONNECTING' || moodConfig.mood === 'THINKING') return '/assets/thinking.mp4';
    return '/assets/idle.mp4';
  };

  return (
    <div className="relative w-72 h-72 rounded-3xl overflow-hidden border border-white/15 bg-black/40 backdrop-blur-xl shadow-2xl flex items-center justify-center">
      {!videoError ? (
        <video
          key={getVideoSrc()}
          autoPlay
          loop
          muted
          playsInline
          className="w-full h-full object-cover"
          onError={() => setVideoError(true)}
        >
          <source src={getVideoSrc()} type="video/mp4" />
        </video>
      ) : (
        // Animated CSS Fallback
        <div className="flex flex-col items-center justify-center p-6 text-center">
          <div
            className="w-32 h-32 rounded-full flex items-center justify-center mb-4 transition-all duration-700 animate-pulse"
            style={{
              background: `radial-gradient(circle, ${moodConfig.auraColor} 20%, ${moodConfig.secondaryAura} 90%)`,
              boxShadow: `0 0 35px ${moodConfig.auraColor}`
            }}
          >
            <span className="text-5xl">{moodConfig.emoji}</span>
          </div>
          <h3 className="text-white font-medium text-base">MYRAA</h3>
          <p className="text-xs text-white/60 mt-1">{moodConfig.statusTag}</p>
        </div>
      )}

      {/* Floating Mood Badge */}
      <div
        className="absolute top-3 right-3 px-3 py-1 rounded-full text-xs font-semibold backdrop-blur-md border border-white/20 text-white shadow-lg flex items-center gap-1.5"
        style={{ backgroundColor: `${moodConfig.auraColor}33` }}
      >
        <span>{moodConfig.emoji}</span>
        <span>{moodConfig.mood}</span>
      </div>

      {/* Connection Indicator */}
      <div className="absolute bottom-3 left-3 flex items-center gap-2 px-2.5 py-1 rounded-full bg-black/60 border border-white/10 text-[11px] text-white/80">
        <span
          className={`w-2 h-2 rounded-full ${
            connectionState === 'SPEAKING'
              ? 'bg-emerald-400 animate-ping'
              : connectionState === 'LISTENING'
              ? 'bg-purple-400 animate-pulse'
              : 'bg-amber-400'
          }`}
        />
        <span>{connectionState}</span>
      </div>
    </div>
  );
};
