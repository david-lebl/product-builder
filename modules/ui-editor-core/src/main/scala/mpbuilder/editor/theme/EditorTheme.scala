package mpbuilder.editor.theme

/** Visual Editor stylesheet.
  *
  * Written as plain CSS (injected once by the browser host) because the workspace relies on features typed
  * `kyo.Style` cannot express: CSS grid areas, `pointer-events`, per-handle resize cursors, `touch-action`,
  * `vector-effect: non-scaling-stroke` for zoom-independent guides, and sizing rules for injected SVG. Every
  * colour comes from the shared `--mp-*` design tokens, so light / dark mode follow the storefront theme.
  */
object EditorTheme:

  val css: String =
    """.ve-root{position:fixed;inset:0;z-index:90;display:flex;flex-direction:column;background:var(--mp-canvas);color:var(--mp-ink);font-family:Inter,system-ui,-apple-system,"Segoe UI",sans-serif;font-size:14px}
      |.ve-root.ve-embedded{position:relative;inset:auto;height:100vh;height:100dvh;z-index:auto}
      |.ve-root *{box-sizing:border-box}
      |.ve-workspace{display:flex;flex-direction:column;flex:1 1 auto;height:100%;min-height:0}
      |.ve-topbar{flex-direction:row;flex:none;height:58px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--mp-border);background:var(--mp-surface)}
      |.ve-topbar-spacer{flex:1}
      |.ve-name-input{min-width:120px;max-width:260px;flex:0 1 240px;font-weight:700;font-size:15px;padding:6px 10px;border:1px solid transparent;border-radius:8px;background:transparent;color:var(--mp-ink)}
      |.ve-name-input:hover{border-color:var(--mp-border)}
      |.ve-name-input:focus{border-color:var(--mp-brand);outline:none;background:var(--mp-surface)}
      |.ve-format-chip{font-size:12px;color:var(--mp-ink-muted);padding:4px 10px;border-radius:999px;background:var(--mp-surface-alt);white-space:nowrap}
      |.ve-body{flex-direction:row;flex:1;display:flex;min-height:0;position:relative}
      |.ve-rail{flex:none;width:76px;display:flex;flex-direction:column;gap:4px;padding:10px 6px;border-right:1px solid var(--mp-border);background:var(--mp-surface);overflow-y:auto}
      |.ve-rail-btn{display:flex;flex-direction:column;align-items:center;gap:4px;padding:9px 2px;border-radius:12px;border:0;background:transparent;color:var(--mp-ink-secondary);font-size:11px;font-weight:600;cursor:pointer}
      |.ve-rail-btn:hover{background:var(--mp-surface-hover);color:var(--mp-ink)}
      |.ve-rail-btn-active{background:var(--mp-brand-soft);color:var(--mp-brand)}
      |.ve-panel{flex:none;width:296px;overflow-y:auto;border-right:1px solid var(--mp-border);background:var(--mp-surface);padding:14px 16px 24px;display:flex;flex-direction:column;gap:14px}
      |.ve-panel-head,.ve-inspector-head{flex-direction:row;display:flex;align-items:center;justify-content:space-between;gap:8px}
      |.ve-panel-title{margin:0;font-size:15px;font-weight:700}
      |.ve-stage{flex:1;display:flex;flex-direction:column;min-width:0;min-height:0}
      |.ve-canvas{flex:1;overflow:auto;display:flex;align-items:safe center;justify-content:safe center;padding:28px;min-height:0;touch-action:none;background-color:var(--mp-canvas);background-image:radial-gradient(var(--mp-border-strong) 1px,transparent 1px);background-size:20px 20px;user-select:none;-webkit-user-select:none}
      |.ve-page-wrap{position:relative;flex:none;margin:auto;box-shadow:0 1px 2px rgba(15,23,42,.08),0 12px 32px -8px rgba(15,23,42,.28);background:#fff}
      |.ve-page-art,.ve-page-art>svg{position:absolute;inset:0;width:100%;height:100%;display:block}
      |.ve-page-art [data-ve-el]{cursor:move}
      |.ve-page-art [data-ve-locked]{cursor:default}
      |.ve-overlay{position:absolute;inset:0;width:100%;height:100%;pointer-events:none;overflow:visible}
      |.ve-overlay *{vector-effect:non-scaling-stroke}
      |.ve-bleed-shade{fill:rgba(220,38,38,.10);stroke:none}
      |.ve-trim-line{stroke:rgba(220,38,38,.75);stroke-width:1}
      |.ve-safe-line{stroke:rgba(79,70,229,.55);stroke-width:1}
      |.ve-binding-zone{fill:rgba(100,116,139,.18);stroke:rgba(100,116,139,.6);stroke-width:1;stroke-dasharray:3 3}
      |.ve-snap-line{stroke:#ec4899;stroke-width:1}
      |.ve-sel-outline{fill:none;stroke:var(--mp-brand);stroke-width:1.5}
      |.ve-sel-locked{stroke:var(--mp-ink-muted);stroke-dasharray:4 3}
      |.ve-rotate-stem{stroke:var(--mp-brand);stroke-width:1}
      |.ve-handle{pointer-events:all;fill:#fff;stroke:var(--mp-brand);stroke-width:1.5;touch-action:none}
      |.ve-handle-n,.ve-handle-s{cursor:ns-resize}
      |.ve-handle-e,.ve-handle-w{cursor:ew-resize}
      |.ve-handle-nw,.ve-handle-se{cursor:nwse-resize}
      |.ve-handle-ne,.ve-handle-sw{cursor:nesw-resize}
      |.ve-handle-rotate{cursor:grab;fill:var(--mp-brand);stroke:#fff}
      |.ve-inspector{flex:none;width:300px;overflow-y:auto;border-left:1px solid var(--mp-border);background:var(--mp-surface);padding:14px 16px 28px}
      |.ve-pagestrip{flex-direction:row;flex:none;display:flex;align-items:center;gap:12px;padding:8px 14px;border-top:1px solid var(--mp-border);background:var(--mp-surface);min-height:112px}
      |.ve-thumbs{flex-direction:row;flex:1;display:flex;gap:10px;overflow-x:auto;padding:4px 2px}
      |.ve-thumb{flex:none;display:flex;flex-direction:column;align-items:center;gap:4px;padding:4px;border:2px solid transparent;border-radius:10px;background:transparent;cursor:pointer;color:var(--mp-ink-secondary)}
      |.ve-thumb:hover{background:var(--mp-surface-hover)}
      |.ve-thumb-active{border-color:var(--mp-brand);color:var(--mp-brand)}
      |.ve-thumb-art{height:66px}
      |.ve-thumb-label{font-size:11px;font-weight:600;white-space:nowrap;max-width:110px;overflow:hidden;text-overflow:ellipsis}
      |.ve-page-actions{flex-direction:row;flex:none;display:flex;gap:4px}
      |.ve-art{display:block;line-height:0}
      |.ve-art>svg{display:block;width:100%;height:100%;filter:drop-shadow(0 1px 4px rgba(15,23,42,.28))}
      |.ve-stack{display:flex;flex-direction:column;gap:12px}
      |.ve-stack-tight{display:flex;flex-direction:column;gap:4px}
      |.ve-row{flex-direction:row;display:flex;align-items:center;gap:8px}
      |.ve-row-tight{gap:2px}
      |.ve-row-wrap{flex-wrap:wrap}
      |.ve-row-between{justify-content:space-between}
      |.ve-grid2{display:grid;grid-template-columns:1fr 1fr;gap:8px 10px}
      |.ve-section{display:flex;flex-direction:column;gap:8px;padding-top:12px;border-top:1px solid var(--mp-border)}
      |.ve-section-title{margin:0;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:var(--mp-ink-muted)}
      |.ve-field{display:flex;flex-direction:column;gap:4px;min-width:0}
      |.ve-field-label{font-size:12px;font-weight:600;color:var(--mp-ink-secondary)}
      |.ve-num,.ve-select{width:100%;padding:6px 8px;font-size:13px}
      |.ve-textarea{width:100%;min-height:84px;resize:vertical;font-size:14px;line-height:1.45;padding:8px 10px}
      |.ve-range{width:100%;accent-color:var(--mp-brand)}
      |.ve-hint{margin:0;font-size:12px;line-height:1.5;color:var(--mp-ink-muted)}
      |.ve-tool{display:inline-flex;align-items:center;justify-content:center;min-width:32px;height:32px;padding:0 8px;border-radius:8px;border:1px solid transparent;background:transparent;color:var(--mp-ink-secondary);font-size:13px;font-weight:700;cursor:pointer}
      |.ve-tool:hover:not(:disabled){background:var(--mp-surface-hover);color:var(--mp-ink)}
      |.ve-tool:disabled{opacity:.35;cursor:default}
      |.ve-tool-active{background:var(--mp-brand-soft);color:var(--mp-brand);border-color:var(--mp-brand-border)}
      |.ve-tool-italic{font-style:italic}
      |.ve-tool-underline{text-decoration:underline}
      |.ve-divider{width:1px;height:22px;background:var(--mp-border);margin:0 2px}
      |.ve-zoom{flex-direction:row;display:flex;align-items:center;gap:2px;padding:2px;border:1px solid var(--mp-border);border-radius:10px}
      |.ve-zoom-value{min-width:58px;border:0;background:transparent;font-size:12px;font-weight:700;color:var(--mp-ink-secondary);cursor:pointer;font-variant-numeric:tabular-nums}
      |.ve-zoom-fit{color:var(--mp-brand)}
      |.ve-status{flex-direction:row;display:inline-flex;align-items:center;gap:4px;font-size:12px;font-weight:600;white-space:nowrap}
      |.ve-status-dirty,.ve-status-saving{color:var(--mp-ink-muted)}
      |.ve-status-saved{color:var(--mp-emerald)}
      |.ve-status-failed{color:var(--mp-rose)}
      |.ve-colors{display:flex;flex-direction:column;gap:8px}
      |.ve-swatches{display:grid;grid-template-columns:repeat(9,1fr);gap:5px}
      |.ve-swatch{aspect-ratio:1;border-radius:6px;border:1px solid var(--mp-border-strong);cursor:pointer;padding:0}
      |.ve-swatch-active{outline:2px solid var(--mp-brand);outline-offset:2px}
      |.ve-color-input{width:100%;height:32px;border:1px solid var(--mp-border);border-radius:8px;background:var(--mp-surface);padding:2px;cursor:pointer}
      |.ve-text-preset{display:block;width:100%;text-align:left;padding:12px 14px;border:1px solid var(--mp-border);border-radius:12px;background:var(--mp-surface-alt);color:var(--mp-ink);cursor:pointer}
      |.ve-text-preset:hover{border-color:var(--mp-brand-border);background:var(--mp-brand-soft)}
      |.ve-text-preset-h{font-family:Montserrat,Inter,sans-serif;font-size:22px;font-weight:800}
      |.ve-text-preset-s{font-family:Montserrat,Inter,sans-serif;font-size:16px;font-weight:600}
      |.ve-text-preset-b{font-size:13px}
      |.ve-text-preset-script{font-family:"Dancing Script",cursive;font-size:22px;font-weight:700}
      |.ve-upload-btn{width:100%}
      |.ve-photo-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:6px}
      |.ve-photo{position:relative;aspect-ratio:1;border-radius:8px;overflow:hidden;border:1px solid var(--mp-border);padding:0;background:var(--mp-surface-alt);cursor:pointer;display:flex;align-items:center;justify-content:center;color:var(--mp-ink-muted)}
      |.ve-photo:hover{border-color:var(--mp-brand)}
      |.ve-photo-img{width:100%;height:100%;object-fit:cover;display:block}
      |.ve-photo-used{position:absolute;top:4px;right:4px;width:18px;height:18px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:var(--mp-emerald);color:#fff}
      |.ve-photo-meta{position:absolute;left:0;right:0;bottom:0;font-size:9px;padding:2px 4px;background:rgba(15,23,42,.55);color:#fff;text-align:left}
      |.ve-empty-photos{display:flex;flex-direction:column;align-items:center;gap:6px;padding:20px;border:1px dashed var(--mp-border-strong);border-radius:12px;color:var(--mp-ink-muted);text-align:center}
      |.ve-empty-photos p{margin:0;font-size:12px}
      |.ve-shape-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:6px}
      |.ve-shape-btn{display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 2px;border:1px solid var(--mp-border);border-radius:10px;background:var(--mp-surface-alt);color:var(--mp-brand);font-size:10px;cursor:pointer}
      |.ve-shape-btn span{color:var(--mp-ink-secondary)}
      |.ve-clipart-grid{display:grid;grid-template-columns:repeat(5,1fr);gap:6px}
      |.ve-clipart-btn{aspect-ratio:1;display:flex;align-items:center;justify-content:center;border:1px solid var(--mp-border);border-radius:10px;background:var(--mp-surface-alt);color:#f97316;cursor:pointer;padding:4px}
      |.ve-shape-btn:hover,.ve-clipart-btn:hover,.ve-layout-btn:hover,.ve-gradient-btn:hover{border-color:var(--mp-brand)}
      |.ve-gradient-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:6px}
      |.ve-gradient-btn{height:44px;border-radius:10px;border:1px solid var(--mp-border);cursor:pointer}
      |.ve-layout-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:8px}
      |.ve-layout-btn{display:flex;flex-direction:column;align-items:center;gap:6px;padding:10px;border:1px solid var(--mp-border);border-radius:12px;background:var(--mp-surface-alt);cursor:pointer;font-size:12px;color:var(--mp-ink-secondary)}
      |.ve-layout-glyph{width:100%;height:76px}
      |.ve-layout-page{fill:#fff;stroke:var(--mp-border-strong);stroke-width:.6}
      |.ve-layout-slot{fill:var(--mp-brand-soft);stroke:var(--mp-brand);stroke-width:.8}
      |.ve-layout-caption{fill:var(--mp-border);stroke:none}
      |.ve-template-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px}
      |.ve-template-card{display:flex;flex-direction:column;gap:6px;padding:8px;border:1px solid var(--mp-border);border-radius:12px;background:var(--mp-surface-alt);cursor:pointer;text-align:left}
      |.ve-template-card:hover{border-color:var(--mp-brand)}
      |.ve-template-card .ve-art{height:104px;width:100%}
      |.ve-template-name{font-size:12px;font-weight:700;color:var(--mp-ink)}
      |.ve-confirm,.ve-callout{display:flex;flex-direction:column;gap:8px;padding:12px;border-radius:12px;background:var(--mp-amber-soft);border:1px solid var(--mp-amber-border);color:var(--mp-amber-ink)}
      |.ve-confirm p,.ve-callout p{margin:0;font-size:13px}
      |.ve-callout-brand{flex-direction:row;flex-direction:row;align-items:center;background:var(--mp-brand-soft);border-color:var(--mp-brand-border);color:var(--mp-brand-ink)}
      |.ve-file-name{font-size:12px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0}
      |.ve-pill{font-size:11px;font-weight:700;padding:2px 8px;border-radius:999px;white-space:nowrap}
      |.ve-pill-good{background:var(--mp-emerald-soft);color:var(--mp-emerald-ink)}
      |.ve-pill-warn{background:var(--mp-amber-soft);color:var(--mp-amber-ink)}
      |.ve-pill-bad{background:var(--mp-rose-soft);color:var(--mp-rose-ink)}
      |.ve-facts{display:grid;grid-template-columns:1fr 1fr;gap:8px}
      |.ve-fact{display:flex;flex-direction:column;gap:2px;padding:8px 10px;border-radius:10px;background:var(--mp-surface-alt)}
      |.ve-fact-label{font-size:11px;color:var(--mp-ink-muted);font-weight:600}
      |.ve-fact-value{font-size:13px;font-weight:700;font-variant-numeric:tabular-nums}
      |.ve-legend{flex-direction:row;display:grid;grid-template-columns:14px 1fr;align-items:center;gap:6px 8px;font-size:12px;color:var(--mp-ink-secondary)}
      |.ve-legend-swatch{width:14px;height:10px;border-radius:2px}
      |.ve-legend-bleed{background:rgba(220,38,38,.25);border:1px solid rgba(220,38,38,.75)}
      |.ve-legend-safe{border:1px dashed rgba(79,70,229,.8)}
      |.ve-check{flex-direction:row;display:flex;align-items:flex-start;gap:8px;width:100%;text-align:left;padding:8px 10px;border-radius:10px;border:1px solid transparent;font-size:12px;line-height:1.4;background:transparent;cursor:pointer;color:inherit}
      |.ve-check-ok{background:var(--mp-emerald-soft);color:var(--mp-emerald-ink);cursor:default}
      |.ve-check-error{background:var(--mp-rose-soft);color:var(--mp-rose-ink);border-color:var(--mp-rose-border)}
      |.ve-check-warn{background:var(--mp-amber-soft);color:var(--mp-amber-ink);border-color:var(--mp-amber-border)}
      |.ve-check-info{background:var(--mp-sky-soft);color:var(--mp-sky)}
      |.ve-check-page{opacity:.75}
      |.ve-review-backdrop{position:fixed;inset:0;z-index:120;display:flex;align-items:center;justify-content:center;padding:20px;background:var(--mp-backdrop)}
      |.ve-review{width:min(920px,100%);max-height:calc(100vh - 40px);overflow-y:auto;display:flex;flex-direction:column;gap:14px;background:var(--mp-surface);border-radius:18px;padding:22px}
      |.ve-review-head{flex-direction:row;display:flex;align-items:center;justify-content:space-between}
      |.ve-review-title{margin:0;font-size:20px;font-weight:800}
      |.ve-review-pages{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px}
      |.ve-review-page{display:flex;flex-direction:column;align-items:center;gap:6px}
      |.ve-review-art{height:190px;width:100%}
      |.ve-review-actions{flex-direction:row;display:flex;flex-wrap:wrap;align-items:center;gap:8px}
      |.ve-toast{position:fixed;z-index:130}
      |.ve-home{flex:1 1 auto;height:100%;overflow-y:auto;padding:28px clamp(16px,4vw,48px) 48px;display:flex;flex-direction:column;gap:26px}
      |.ve-home-head{flex-direction:row;display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}
      |.ve-home-brand{flex-direction:row;display:flex;align-items:center;gap:14px}
      |.ve-home-logo{width:44px;height:44px;border-radius:14px;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#4f46e5,#db2777);color:#fff}
      |.ve-home-title{margin:0;font-size:26px;font-weight:800;letter-spacing:-.02em}
      |.ve-home-section{display:flex;flex-direction:column;gap:12px}
      |.ve-home-h2{margin:0;font-size:18px;font-weight:700}
      |.ve-kind-row{flex-direction:row;display:flex;flex-wrap:wrap;gap:8px}
      |.ve-kind{display:inline-flex;align-items:center;gap:8px;padding:9px 14px;border-radius:999px;border:1px solid var(--mp-border);background:var(--mp-surface);color:var(--mp-ink-secondary);font-weight:600;font-size:13px;cursor:pointer}
      |.ve-kind:hover{border-color:var(--mp-brand-border)}
      |.ve-kind-active{background:var(--mp-brand);border-color:var(--mp-brand);color:#fff}
      |.ve-card-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:16px}
      |.ve-design-card,.ve-template-tile{display:flex;flex-direction:column;gap:8px;padding:10px;border-radius:16px;border:1px solid var(--mp-border);background:var(--mp-surface);text-align:left;color:var(--mp-ink)}
      |.ve-template-tile{cursor:pointer}
      |.ve-template-tile:hover,.ve-design-card:hover{border-color:var(--mp-brand-border);box-shadow:0 10px 24px -12px var(--mp-shadow-lg)}
      |.ve-design-open{padding:0;border:0;background:transparent;cursor:pointer}
      |.ve-card-art{height:150px;width:100%;border-radius:10px;background:var(--mp-surface-alt);padding:12px}
      |.ve-blank-art{height:150px;display:flex;align-items:center;justify-content:center;border-radius:10px;border:2px dashed var(--mp-border-strong);color:var(--mp-ink-muted)}
      |.ve-design-meta{flex-direction:row;display:flex;align-items:flex-start;justify-content:space-between;gap:6px}
      |.ve-design-name{font-size:14px;font-weight:700}
      |.ve-loading{position:fixed;inset:0;z-index:95;display:flex;align-items:center;justify-content:center;background:var(--mp-backdrop);color:#fff;font-weight:600}
      |@media (max-width:1200px){.ve-inspector{width:264px}.ve-panel{width:264px}.ve-hide-md{display:none}}
      |@media (max-width:960px){
      |  .ve-inspector{position:absolute;right:0;bottom:0;top:auto;max-height:46%;width:100%;z-index:5;border-left:0;border-top:1px solid var(--mp-border);box-shadow:0 -12px 30px -16px var(--mp-shadow-lg)}
      |  .ve-inspector:not(.ve-inspector-has-selection){display:none}
      |  .ve-panel{position:absolute;left:76px;top:0;bottom:0;z-index:6;box-shadow:12px 0 30px -18px var(--mp-shadow-lg)}
      |}
      |@media (max-width:640px){
      |  .ve-body{flex-direction:column-reverse}
      |  .ve-rail{width:100%;flex-direction:row;justify-content:space-around;border-right:0;border-top:1px solid var(--mp-border);padding:4px}
      |  .ve-rail-btn{flex:1;font-size:10px;padding:6px 0}
      |  .ve-panel{left:0;right:0;top:auto;bottom:62px;width:100%;max-height:55%;border-right:0;border-top:1px solid var(--mp-border)}
      |  .ve-inspector{bottom:0}
      |  .ve-hide-sm{display:none}
      |  .ve-canvas{padding:14px}
      |  .ve-pagestrip{min-height:92px}
      |  .ve-topbar{gap:6px;padding:0 8px}
      |  .ve-name-input{flex:1 1 auto;min-width:0}
      |}
      |""".stripMargin
