import { useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { MusicLinkView } from "./types";

type Props = {
  music: MusicLinkView;
};

interface AudioTrack {
  name: string;
  artist: string;
  cover: string;
  /** Server-proxied audio URL — browser gets stable same-origin URL, server handles CDN token refresh */
  streamUrl: string;
}

function formatTime(s: number): string {
  if (!isFinite(s) || isNaN(s)) return "00:00";
  const m = Math.floor(s / 60);
  const sec = Math.floor(s % 60);
  return `${m.toString().padStart(2, "0")}:${sec.toString().padStart(2, "0")}`;
}

async function resolveMetaViaMeting(server: string, type: string, id: string): Promise<AudioTrack[]> {
  const res = await fetch(
    `/meting/api?server=${server}&type=${type}&id=${id}`,
  );
  const data = await res.json();
  const list = Array.isArray(data) ? data : [data];
  return list.map((item: Record<string, string>) => ({
    name: item.name || item.title || "未知歌曲",
    artist: item.artist || item.author || "未知艺术家",
    cover: item.pic || item.cover || "",
    // Proxy through our Java server to avoid cross-origin CDN token expiry
    streamUrl: `/api/v1/audio/stream?server=${server}&id=${item.id || id}`,
  }));
}

export default function MomentMusicPlayer({ music }: Props) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [playbackError, setError] = useState(false);

  const { data: metingTracks = [], isLoading, isError } = useQuery({
    queryKey: ["momentMusic", music.server, music.type, music.id],
    queryFn: () => resolveMetaViaMeting(music.server!, music.type!, music.id!),
    enabled: Boolean(music.server && music.type && music.id),
    staleTime: 10 * 60 * 1000,
  });

  const tracks: AudioTrack[] = useMemo(() => {
    if (metingTracks.length > 0) return metingTracks;
    if (music.url) {
      return [{
        name: music.title || "未知歌曲",
        artist: music.artist || "",
        cover: music.cover || "",
        streamUrl: music.url,
      }];
    }
    return [];
  }, [metingTracks, music.url, music.title, music.artist, music.cover]);

  const loading = isLoading;
  const error = (isError || playbackError) && tracks.length === 0;

  const current = tracks[currentIdx];

  const togglePlay = () => {
    if (!audioRef.current || !current?.streamUrl) return;
    if (playing) {
      audioRef.current.pause();
    } else {
      // Set src each time in case URL needs to be re-resolved
      if (audioRef.current.src !== current.streamUrl || audioRef.current.readyState === 0) {
        audioRef.current.src = current.streamUrl;
      }
      audioRef.current.play().catch(() => setError(true));
    }
  };

  const onEnded = () => {
    if (currentIdx + 1 < tracks.length) {
      setCurrentIdx(currentIdx + 1);
    } else {
      setPlaying(false);
      setCurrentTime(0);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center gap-2 py-3 px-3 text-xs" style={{ color: "var(--muted-ink)" }}>
        ♪ 加载中…
      </div>
    );
  }

  if (error || !current?.streamUrl) {
    if (music.url) {
      return (
        <a href={music.url} target="_blank" rel="noopener noreferrer"
          className="flex items-center gap-3 p-3 border rounded-sm hover:opacity-80"
          style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}>
          {music.cover ? (
            <img src={music.cover} alt="" className="w-10 h-10 object-cover rounded-sm shrink-0" />
          ) : (
            <div className="w-10 h-10 shrink-0 flex items-center justify-center rounded-sm" style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}>♪</div>
          )}
          <div className="min-w-0">
            <div className="text-sm truncate" style={{ color: "var(--ink)" }}>{music.title}</div>
            {music.artist && <div className="text-xs" style={{ color: "var(--muted-ink)" }}>{music.artist}</div>}
          </div>
          <span className="text-xs shrink-0" style={{ color: "var(--muted-ink)" }}>前往收听 →</span>
        </a>
      );
    }
    return (
      <div className="py-3 px-3 text-xs" style={{ color: "var(--muted-ink)" }}>音乐加载失败</div>
    );
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div
      className="flex items-center gap-3 p-3 border rounded-sm"
      style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}
    >
      <audio
        ref={audioRef}
        src={current.streamUrl}
        preload="metadata"
        onTimeUpdate={() => { if (audioRef.current) setCurrentTime(audioRef.current.currentTime); }}
        onLoadedMetadata={() => { if (audioRef.current) setDuration(audioRef.current.duration); }}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
        onEnded={onEnded}
        onError={() => setError(true)}
      />

      {/* Cover + play overlay */}
      <div className="relative shrink-0">
        <div className="w-12 h-12 rounded-sm overflow-hidden" style={{ background: "var(--section-bg)" }}>
          {current.cover ? (
            <img src={current.cover} alt="" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center" style={{ color: "var(--muted-ink)" }}>♪</div>
          )}
        </div>
        <button
          onClick={togglePlay}
          className="absolute inset-0 flex items-center justify-center bg-black/30 text-white rounded-sm opacity-0 hover:opacity-100 transition-opacity"
          aria-label={playing ? "暂停" : "播放"}
        >
          {playing ? "⏸" : "▶"}
        </button>
      </div>

      {/* Track info + progress */}
      <div className="flex-1 min-w-0">
        <div className="text-sm truncate" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>
          {current.name}
        </div>
        {current.artist && (
          <div className="text-xs mt-0.5" style={{ color: "var(--muted-ink)" }}>
            {current.artist}
          </div>
        )}
        <div className="flex items-center gap-2 mt-2">
          <div
            className="flex-1 h-1 rounded-full cursor-pointer"
            style={{ background: "var(--section-bg)" }}
            onClick={(e) => {
              if (!audioRef.current || !duration) return;
              const rect = e.currentTarget.getBoundingClientRect();
              audioRef.current.currentTime = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)) * duration;
            }}
          >
            <div className="h-full rounded-full" style={{ width: `${progress}%`, background: "var(--olive)" }} />
          </div>
          <span className="text-xs shrink-0" style={{ color: "var(--muted-ink)" }}>{formatTime(currentTime)}</span>
        </div>
      </div>
    </div>
  );
}
