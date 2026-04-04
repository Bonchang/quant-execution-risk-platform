import { useMode } from '../lib/mode/ModeContext';

export function ModeToggle() {
  const { mode, setMode } = useMode();

  return (
    <div className="mode-toggle" role="tablist" aria-label="앱 모드">
      <button
        className={mode === 'invest' ? 'mode-toggle__button is-active' : 'mode-toggle__button'}
        type="button"
        onClick={() => setMode('invest')}
      >
        일반 투자
      </button>
      <button
        className={mode === 'quant' ? 'mode-toggle__button is-active' : 'mode-toggle__button'}
        type="button"
        onClick={() => setMode('quant')}
      >
        퀀트 모드
      </button>
    </div>
  );
}
