import React, { useState, useEffect } from 'react';
import { SlidersHorizontal, Sparkles, X, Clock, Zap, Check, ExternalLink, Lock, Power } from 'lucide-react';

type ScrollMode = 'timer' | 'auto_detect';

export default function App() {
  // Configuration State
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [showAppLaunchModal, setShowAppLaunchModal] = useState(false);
  const [isRunning, setIsRunning] = useState(false);

  // Scroll Interval (Direct Typing in Seconds)
  const [scrollIntervalSeconds, setScrollIntervalSeconds] = useState<number>(30);

  // Floating Bubble
  const [floatingBubbleEnabled, setFloatingBubbleEnabled] = useState(true);
  const [floatingBubblePos, setFloatingBubblePos] = useState({ y: 320 });

  // Accessibility Service Permission State
  const [isAccessibilityEnabled, setIsAccessibilityEnabled] = useState(true);

  // Active Timer Countdown
  const [secondsRemaining, setSecondsRemaining] = useState<number>(30);
  const [isSwipingNow, setIsSwipingNow] = useState(false);
  const [totalSwipes, setTotalSwipes] = useState(0);

  // Toast for feedback
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 2500);
  };

  const openAccessibilitySettings = () => {
    const next = !isAccessibilityEnabled;
    setIsAccessibilityEnabled(next);
    showToast(`Opening Accessibility Settings (${next ? 'ON' : 'OFF'})...`);
    
    // Call Android native bridge
    try {
      if ((window as any).AndroidBridge?.openAccessibilitySettings) {
        (window as any).AndroidBridge.openAccessibilitySettings();
        return;
      } else if ((window as any).ShortsAccessibility?.openAccessibilitySettings) {
        (window as any).ShortsAccessibility.openAccessibilitySettings();
        return;
      }
    } catch (_) {}

    try {
      window.location.href = 'intent:#Intent;action=android.settings.ACCESSIBILITY_SETTINGS;end';
    } catch (_) {}
  };

  const getEffectiveInterval = () => {
    return Math.max(3, scrollIntervalSeconds || 30);
  };

  // Expose triggerReelScroll to window for Android native AccessibilityNodeInfo / ContentObserver callbacks
  useEffect(() => {
    (window as any).triggerReelScroll = (reason: string) => {
      triggerReelScroll(reason || 'Android Progress Complete');
    };

    try {
      if ((window as any).AndroidBridge?.setAutoScrollMode) {
        (window as any).AndroidBridge.setAutoScrollMode('timer');
      }
    } catch (_) {}

    return () => {
      delete (window as any).triggerReelScroll;
    };
  }, []);

  // Trigger manual or automatic scroll connected to Accessibility Service
  const triggerReelScroll = (reason: string = 'Ʀ Button Tap') => {
    setIsSwipingNow(true);
    setTotalSwipes((prev) => prev + 1);

    // Haptic feedback
    try {
      if ('vibrate' in navigator) {
        navigator.vibrate([20, 30, 20]);
      }
    } catch (_) {}

    // Invoke Android Accessibility Service bridge if running in Android app
    try {
      if ((window as any).AndroidBridge?.swipeNextShort) {
        (window as any).AndroidBridge.swipeNextShort();
      } else if ((window as any).ShortsAccessibility?.swipeNextShort) {
        (window as any).ShortsAccessibility.swipeNextShort();
      }
    } catch (_) {}

    showToast(`↑ Ʀ Scroll (${reason})`);
    
    // Reset timer
    setSecondsRemaining(getEffectiveInterval());

    setTimeout(() => {
      setIsSwipingNow(false);
    }, 400);
  };

  // Timer loop when running
  useEffect(() => {
    if (!isRunning) return;

    setSecondsRemaining(getEffectiveInterval());

    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          triggerReelScroll('Timer Completed');
          return getEffectiveInterval();
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isRunning, scrollIntervalSeconds]);

  // Handle center START / STOP toggle
  const handleStartToggle = () => {
    if (isRunning) {
      setIsRunning(false);
      showToast('Auto-Scroll Turned OFF');
    } else {
      setIsRunning(true);
      setSecondsRemaining(getEffectiveInterval());
      showToast(`Auto-Scroll Turned ON (${getEffectiveInterval()}s timer)`);
      
      try {
        if ((window as any).AndroidBridge?.startAutoScroll) {
          (window as any).AndroidBridge.startAutoScroll('timer', getEffectiveInterval());
        }
      } catch (_) {}
    }
  };

  // Handle Save / Done in Configuration Modal
  const handleConfigDone = () => {
    setShowConfigModal(false);
    setIsRunning(true);
    setSecondsRemaining(getEffectiveInterval());
    showToast(`Saved! Auto-Scroll active (${getEffectiveInterval()}s).`);
    setShowAppLaunchModal(true);
  };

  const openApp = (app: 'youtube' | 'instagram') => {
    setShowAppLaunchModal(false);
    if (app === 'youtube') {
      showToast('Opening YouTube Shorts...');
      try {
        if ((window as any).AndroidBridge?.openYouTube) {
          (window as any).AndroidBridge.openYouTube();
          return;
        }
      } catch (_) {}
      try {
        window.location.href = 'vnd.youtube:shorts';
      } catch (_) {}
      window.open('https://www.youtube.com/shorts', '_blank');
    } else {
      showToast('Opening Instagram Reels...');
      try {
        if ((window as any).AndroidBridge?.openInstagram) {
          (window as any).AndroidBridge.openInstagram();
          return;
        }
      } catch (_) {}
      try {
        window.location.href = 'instagram://reels';
      } catch (_) {}
      window.open('https://www.instagram.com/reels', '_blank');
    }
  };

  const openInstagramProfile = (e?: React.MouseEvent) => {
    if (e) e.preventDefault();
    const targetUrl = 'https://www.instagram.com/ru_rider__?rpxt=MWJyYmdiZjJqZzJ4dQ==';
    showToast('Opening Instagram (@ru_rider__)...');
    try {
      if ((window as any).AndroidBridge?.openInstagramProfile) {
        (window as any).AndroidBridge.openInstagramProfile(targetUrl);
        return;
      }
    } catch (_) {}
    try {
      window.open(targetUrl, '_blank', 'noopener,noreferrer');
    } catch (_) {
      window.location.href = targetUrl;
    }
  };

  const handleAutoDetectClick = () => {
    showToast('Auto-Detect is disabled. Timer mode is active.');
  };

  return (
    <div className="min-h-screen w-full bg-[#080C14] text-white flex flex-col items-center justify-between font-sans selection:bg-rose-500/30 overflow-x-hidden relative">
      
      {/* Background on full screen: 100% visible, crystal-clear Shinchan picture */}
      <div 
        className="absolute inset-0 z-0 bg-cover bg-center pointer-events-none"
        style={{
          backgroundImage: `url('./assets/aistudio/image_dbb3e17b.png')`,
        }}
      />

      {/* Toast Feedback */}
      {toastMsg && (
        <div className="fixed top-5 z-50 px-4 py-2 rounded-full bg-black/60 border border-sky-400/80 text-sky-200 text-xs shadow-2xl backdrop-blur-xl animate-fade-in flex items-center space-x-2">
          <Sparkles className="w-3.5 h-3.5 text-sky-400" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Main Container: Transparent glass phone container */}
      <div 
        className="relative z-10 w-full max-w-sm flex-1 flex flex-col justify-between p-6 bg-cover bg-center bg-no-repeat shadow-2xl border-x border-white/10 overflow-hidden"
        style={{
          backgroundImage: `url('./assets/aistudio/image_dbb3e17b.png')`,
        }}
      >
        {/* Soft transparent top and bottom gradients */}
        <div className="absolute inset-x-0 top-0 h-20 bg-gradient-to-b from-black/25 to-transparent pointer-events-none" />
        <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/35 to-transparent pointer-events-none" />

        {/* ============================================================== */}
        {/* TOP HEADER: Transparent Glass Bar with Title & Instagram Ʀ     */}
        {/* ============================================================== */}
        <div className="relative z-10 w-full flex items-center justify-between p-3 rounded-2xl bg-black/25 backdrop-blur-md border border-white/20 shadow-lg">
          
          {/* Title: ZAND JINDAGI (Transparent Frosted Glass Text) */}
          <div className="flex flex-col">
            <span className="text-2xl font-black tracking-wider uppercase select-none text-transparent bg-clip-text bg-gradient-to-r from-white/95 via-pink-200/80 to-white/70 drop-shadow-[0_0_15px_rgba(255,42,133,0.7)] leading-none">
              ZAND JINDAGI
            </span>
            <h1 className="text-xs font-semibold text-white/80 tracking-wider select-none leading-tight mt-1 drop-shadow">
              Reels &amp; Shorts Auto-Scroll
            </h1>
          </div>

          {/* Right Header: Ʀ Icon - Transparent Glass, opens Instagram Profile */}
          <div className="flex items-center space-x-2">
            <a
              href="https://www.instagram.com/ru_rider__?rpxt=MWJyYmdiZjJqZzJ4dQ=="
              target="_blank"
              rel="noopener noreferrer"
              onClick={openInstagramProfile}
              className="w-10 h-10 rounded-xl border border-sky-400/70 flex items-center justify-center transition-all cursor-pointer active:scale-95 select-none bg-sky-500/20 backdrop-blur-md text-sky-300 shadow-[0_0_16px_rgba(56,189,248,0.5)] hover:bg-sky-500/35 hover:shadow-[0_0_24px_rgba(56,189,248,0.9)] hover:scale-105 group"
              title="Open Instagram Profile: @ru_rider__"
            >
              <span className="text-lg font-black leading-none drop-shadow-[0_0_8px_rgba(56,189,248,0.9)] group-hover:text-white transition-colors">
                Ʀ
              </span>
            </a>
          </div>

        </div>

        {/* ============================================================== */}
        {/* CENTER SECTION: Open viewport showing transparent artwork      */}
        {/* ============================================================== */}
        <div className="relative z-10 flex-1 flex flex-col items-center justify-center my-4" />

        {/* ============================================================== */}
        {/* BOTTOM SECTION: EDIT + ON/OFF BUTTON + 2 APP ICONS + FOOTER   */}
        {/* ============================================================== */}
        <div className="relative z-10 w-full flex flex-col items-center space-y-3 pt-2">
          
          {/* EDIT Button + ON / OFF Toggle Button side by side */}
          <div className="flex items-center space-x-3">
            {/* Transparent Glowing EDIT Button */}
            <button
              type="button"
              onClick={() => setShowConfigModal(true)}
              className="px-4 py-2 rounded-full bg-black/35 backdrop-blur-md border border-[#FF2A85]/70 text-[#FF2A85] text-xs font-black tracking-widest flex items-center space-x-1.5 shadow-[0_0_15px_rgba(255,42,133,0.35)] hover:bg-[#FF2A85]/20 hover:shadow-[0_0_24px_rgba(255,42,133,0.7)] hover:scale-105 active:scale-95 transition-all cursor-pointer select-none"
              title="Configure Timer Settings"
            >
              <SlidersHorizontal className="w-3.5 h-3.5 text-[#FF2A85]" />
              <span>EDIT</span>
            </button>

            {/* ON / OFF Toggle Button right next to EDIT */}
            <button
              type="button"
              onClick={handleStartToggle}
              className={`px-4 py-2 rounded-full backdrop-blur-md text-xs font-black tracking-wider flex items-center space-x-1.5 transition-all cursor-pointer select-none border active:scale-95 ${
                isRunning
                  ? 'bg-emerald-500/25 border-emerald-400/80 text-emerald-300 shadow-[0_0_18px_rgba(52,211,153,0.6)] hover:bg-emerald-500/35 hover:scale-105'
                  : 'bg-black/35 border-white/20 text-slate-400 hover:text-white hover:border-white/35 hover:scale-105'
              }`}
              title={isRunning ? 'Auto-Scroll Active (Tap to Turn OFF)' : 'Auto-Scroll Paused (Tap to Turn ON)'}
            >
              <Power className={`w-3.5 h-3.5 ${isRunning ? 'text-emerald-400 stroke-[2.5]' : 'text-slate-400 stroke-2'}`} />
              <span>{isRunning ? 'ON' : 'OFF'}</span>
            </button>
          </div>

          {/* 2 Transparent App Buttons: Instagram & YouTube */}
          <div className="w-full grid grid-cols-2 gap-3">
            
            {/* 1. YouTube Button (Transparent Glass) */}
            <button
              type="button"
              onClick={() => openApp('youtube')}
              className="py-3 px-3 rounded-2xl bg-red-600/30 hover:bg-red-600/50 backdrop-blur-md text-white flex items-center justify-center space-x-2 font-bold text-xs shadow-lg border border-red-400/40 hover:scale-102 active:scale-95 transition-all cursor-pointer"
              title="Open YouTube Shorts"
            >
              <svg className="w-4 h-4 fill-current shrink-0 drop-shadow" viewBox="0 0 24 24">
                <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
              </svg>
              <span className="drop-shadow">YouTube</span>
            </button>

            {/* 2. Instagram Button (Transparent Glass) */}
            <button
              type="button"
              onClick={() => openApp('instagram')}
              className="py-3 px-3 rounded-2xl bg-gradient-to-r from-pink-600/30 via-rose-600/30 to-orange-500/30 hover:from-pink-600/50 hover:to-orange-500/50 backdrop-blur-md text-white flex items-center justify-center space-x-2 font-bold text-xs shadow-lg border border-pink-400/40 hover:scale-102 active:scale-95 transition-all cursor-pointer"
              title="Open Instagram Reels"
            >
              <svg className="w-4 h-4 fill-none stroke-current stroke-2 shrink-0 drop-shadow" viewBox="0 0 24 24">
                <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
              </svg>
              <span className="drop-shadow">Instagram</span>
            </button>

          </div>

          {/* Footer: Created by Rider */}
          <div className="pt-2 pb-0.5 select-none">
            <span className="text-[9px] font-semibold tracking-[0.25em] text-white/80 uppercase drop-shadow">
              Created by Rider
            </span>
          </div>

        </div>

      </div>

      {/* ============================================================== */}
      {/* FLOATING Ʀ BUBBLE (Transparent Glass)                           */}
      {/* ============================================================== */}
      {floatingBubbleEnabled && (
        <div 
          className="fixed right-4 z-40 flex flex-col items-center select-none"
          style={{ top: `${floatingBubblePos.y}px` }}
        >
          <button
            type="button"
            onClick={() => triggerReelScroll('Ʀ Bubble Tap')}
            className={`w-13 h-13 rounded-2xl bg-black/35 backdrop-blur-md border-2 border-sky-400/80 text-sky-400 flex flex-col items-center justify-center shadow-[0_0_20px_rgba(56,189,248,0.5)] hover:shadow-[0_0_30px_rgba(56,189,248,0.85)] hover:bg-sky-950/45 cursor-pointer hover:scale-105 active:scale-90 transition-all relative ${
              isSwipingNow ? 'translate-y-[-14px] scale-110 shadow-[0_0_35px_#38BDF8] border-white' : ''
            }`}
            title="Tap Ʀ to Auto-Scroll Reel / Short"
          >
            {/* Same Ʀ app icon as top header */}
            <span className="text-2xl font-black text-sky-400 select-none leading-none drop-shadow-[0_0_10px_rgba(56,189,248,0.9)]">
              Ʀ
            </span>

            {/* Countdown Badge inside Bubble if Timer is Active */}
            {isRunning && (
              <span className="absolute -top-1.5 -left-1.5 min-w-[20px] h-5 px-1 rounded-full bg-sky-500/90 border border-white text-white font-mono font-bold text-[9px] flex items-center justify-center shadow">
                {secondsRemaining >= 60 ? `${Math.ceil(secondsRemaining / 60)}m` : `${secondsRemaining}s`}
              </span>
            )}
          </button>
        </div>
      )}

      {/* ============================================================== */}
      {/* ULTRA-TRANSPARENT CONFIGURATION MODAL (Type Time Directly)     */}
      {/* ============================================================== */}
      {showConfigModal && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-black/30 backdrop-blur-xl border border-white/20 rounded-3xl w-full max-w-sm p-6 shadow-[0_8px_32px_0_rgba(0,0,0,0.6)] space-y-5 animate-scale-in">
            
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div className="flex items-center space-x-2">
                <SlidersHorizontal className="w-5 h-5 text-sky-400" />
                <span className="text-base font-bold text-white tracking-wide drop-shadow">
                  Set Scroll Timer
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowConfigModal(false)}
                className="text-white/60 hover:text-white p-1 cursor-pointer transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* 30s, 1m & Small Input (Ultra-Clean, Zero Clutter) */}
            <div className="flex items-center justify-center gap-3 py-3">
              <button
                type="button"
                onClick={() => setScrollIntervalSeconds(30)}
                className={`px-4 py-2 rounded-xl text-sm font-bold border transition-all cursor-pointer ${
                  scrollIntervalSeconds === 30
                    ? 'bg-sky-500/40 border-sky-400 text-white shadow-[0_0_12px_rgba(56,189,248,0.5)]'
                    : 'bg-black/30 border-white/20 text-white/80 hover:bg-white/10 hover:text-white'
                }`}
              >
                30s
              </button>

              <button
                type="button"
                onClick={() => setScrollIntervalSeconds(60)}
                className={`px-4 py-2 rounded-xl text-sm font-bold border transition-all cursor-pointer ${
                  scrollIntervalSeconds === 60
                    ? 'bg-sky-500/40 border-sky-400 text-white shadow-[0_0_12px_rgba(56,189,248,0.5)]'
                    : 'bg-black/30 border-white/20 text-white/80 hover:bg-white/10 hover:text-white'
                }`}
              >
                1m
              </button>

              <input
                type="number"
                min={1}
                max={600}
                value={scrollIntervalSeconds || ''}
                onChange={(e) => {
                  const v = parseInt(e.target.value, 10);
                  setScrollIntervalSeconds(isNaN(v) ? 0 : Math.max(1, v));
                }}
                placeholder="s"
                className="w-16 py-2 px-2 rounded-xl bg-black/40 border border-white/25 focus:border-sky-400 focus:outline-none text-white font-mono font-bold text-center text-sm shadow-inner"
              />
            </div>

            {/* ACTION: SAVE & START BUTTON */}
            <div className="pt-2">
              <button
                type="button"
                onClick={handleConfigDone}
                className="w-full py-3 rounded-xl bg-sky-500/35 hover:bg-sky-500/55 backdrop-blur-md border border-sky-400 text-white font-bold text-sm tracking-wider shadow-lg active:scale-98 transition-all cursor-pointer"
              >
                Save &amp; Start
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ============================================================== */}
      {/* TRANSPARENT QUICK LAUNCH MODAL (Window)                        */}
      {/* ============================================================== */}
      {showAppLaunchModal && (
        <div className="fixed inset-0 z-50 bg-black/55 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-black/50 backdrop-blur-2xl border border-white/20 rounded-3xl w-full max-w-sm p-5 shadow-[0_8px_32px_0_rgba(0,0,0,0.7)] space-y-4 animate-scale-in text-center">
            
            <div className="w-12 h-12 rounded-full bg-emerald-500/20 border border-emerald-400 text-emerald-300 flex items-center justify-center mx-auto shadow-[0_0_15px_rgba(52,211,153,0.4)]">
              <Check className="w-6 h-6 stroke-[3]" />
            </div>

            <div>
              <h3 className="text-base font-bold text-white drop-shadow">Auto-Scroll Activated!</h3>
              <p className="text-xs text-white/80 mt-1 drop-shadow">
                Open YouTube Shorts or Instagram Reels to start auto-scrolling:
              </p>
            </div>

            {/* YouTube and Instagram Launch Buttons (Transparent Glass) */}
            <div className="space-y-2.5 pt-1">
              {/* YouTube Shorts Button */}
              <button
                type="button"
                onClick={() => openApp('youtube')}
                className="w-full py-3 px-4 rounded-2xl bg-red-600/35 hover:bg-red-600/55 backdrop-blur-md border border-red-400/50 text-white font-bold text-xs flex items-center justify-between shadow-lg hover:scale-102 active:scale-95 transition-all cursor-pointer"
              >
                <div className="flex items-center space-x-2.5">
                  <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                    <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
                  </svg>
                  <span>Open YouTube Shorts</span>
                </div>
                <ExternalLink className="w-4 h-4 opacity-80" />
              </button>

              {/* Instagram Reels Button */}
              <button
                type="button"
                onClick={() => openApp('instagram')}
                className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-pink-600/35 via-rose-600/35 to-orange-500/35 hover:from-pink-600/55 hover:to-orange-500/55 backdrop-blur-md border border-pink-400/50 text-white font-bold text-xs flex items-center justify-between shadow-lg hover:scale-102 active:scale-95 transition-all cursor-pointer"
              >
                <div className="flex items-center space-x-2.5">
                  <svg className="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                    <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                    <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                    <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                  </svg>
                  <span>Open Instagram Reels</span>
                </div>
                <ExternalLink className="w-4 h-4 opacity-80" />
              </button>
            </div>

            {/* Dismiss button */}
            <div className="pt-2 border-t border-white/15">
              <button
                type="button"
                onClick={() => setShowAppLaunchModal(false)}
                className="w-full py-2.5 rounded-xl bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/15 text-white/90 font-bold text-xs transition-all cursor-pointer"
              >
                Done (Keep in Background)
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
