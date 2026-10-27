import { afterNextRender, Component, DestroyRef, effect, ElementRef, inject, input, output, signal, untracked, viewChild } from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import TextAlign from '@tiptap/extension-text-align';
import { texts } from '../../i18n/i18n';

interface Tool {
  id: string;
  run: (e: Editor) => void;
  active?: (e: Editor) => boolean;
}

/** Only what a letter needs, and what the PDF keeps: paragraphs, emphasis, lists, alignment. */
const TOOLS: Tool[] = [
  { id: 'bold', run: (e) => e.chain().focus().toggleBold().run(), active: (e) => e.isActive('bold') },
  { id: 'italic', run: (e) => e.chain().focus().toggleItalic().run(), active: (e) => e.isActive('italic') },
  { id: 'underline', run: (e) => e.chain().focus().toggleUnderline().run(), active: (e) => e.isActive('underline') },
  { id: 'bullets', run: (e) => e.chain().focus().toggleBulletList().run(), active: (e) => e.isActive('bulletList') },
  { id: 'numbers', run: (e) => e.chain().focus().toggleOrderedList().run(), active: (e) => e.isActive('orderedList') },
  { id: 'left', run: (e) => e.chain().focus().setTextAlign('left').run(), active: (e) => e.isActive({ textAlign: 'left' }) },
  { id: 'center', run: (e) => e.chain().focus().setTextAlign('center').run(), active: (e) => e.isActive({ textAlign: 'center' }) },
  { id: 'right', run: (e) => e.chain().focus().setTextAlign('right').run(), active: (e) => e.isActive({ textAlign: 'right' }) },
  { id: 'undo', run: (e) => e.chain().focus().undo().run() },
  { id: 'redo', run: (e) => e.chain().focus().redo().run() },
];

/** Formatted text of a letter template (TipTap), as HTML. */
@Component({
  selector: 'lx-rich-editor',
  template: `
    <div class="toolbar" role="toolbar" [attr.aria-label]="tx().templates.editor.toolbar">
      @for (t of tools; track t.id) {
        <button type="button" class="tool" [class]="'tool tool-' + t.id" [class.on]="activeTools().has(t.id)" [attr.aria-pressed]="t.active ? activeTools().has(t.id) : null" [title]="tx().templates.editor.tools[t.id][1]" [attr.aria-label]="tx().templates.editor.tools[t.id][1]" (mousedown)="$event.preventDefault()" (click)="apply(t)">{{ tx().templates.editor.tools[t.id][0] }}</button>
      }
    </div>
    <div #host class="host" [attr.aria-labelledby]="labelledBy()"></div>
  `,
  styles: `
    :host { display: block; border: 1px solid var(--border-strong); border-radius: var(--radius); background: var(--surface); }
    :host(:focus-within) { outline: 2px solid var(--accent); outline-offset: 1px; }
    .toolbar { display: flex; flex-wrap: wrap; gap: 2px; padding: 4px; border-bottom: 1px solid var(--border); background: var(--ground); border-radius: var(--radius) var(--radius) 0 0; }
    .tool { min-width: 32px; height: 30px; padding: 0 6px; border: 1px solid transparent; border-radius: 6px; background: none; cursor: pointer; font-size: 13px; }
    .tool:hover { background: var(--surface); border-color: var(--border); }
    .tool.on { background: var(--tint); border-color: #b9cdea; color: var(--accent-dark); }
    .tool-bold { font-weight: 700; }
    .tool-italic { font-style: italic; }
    .tool-underline { text-decoration: underline; }
    .host { min-height: 260px; max-height: 520px; overflow-y: auto; padding: 10px 14px; }
    .host ::ng-deep .ProseMirror { min-height: 240px; outline: none; font-size: 14px; line-height: 1.5; white-space: pre-wrap; }
    .host ::ng-deep .ProseMirror p { margin: 0 0 0.8em; }
  `,
})
export class RichEditor {
  protected readonly tx = texts;
  /** HTML; replaced when it changes from outside (another template chosen). */
  readonly content = input('');
  readonly labelledBy = input<string | null>(null);
  readonly contentChange = output<string>();

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('host');
  private editor: Editor | null = null;
  /** Last HTML sent or received, to tell our own updates from a new template. */
  private current = '';

  protected readonly tools = TOOLS;
  protected readonly activeTools = signal<Set<string>>(new Set());

  constructor() {
    afterNextRender(() => {
      this.current = this.content();
      this.editor = new Editor({
        element: this.host().nativeElement,
        extensions: [
          StarterKit.configure({ code: false, codeBlock: false, strike: false, link: false, horizontalRule: false, blockquote: false, heading: false }),
          TextAlign.configure({ types: ['paragraph'] }),
        ],
        content: this.current,
        editorProps: { attributes: { 'aria-multiline': 'true', role: 'textbox', 'aria-label': texts().templates.text } },
        onUpdate: ({ editor }) => {
          this.current = editor.getHTML();
          this.contentChange.emit(this.current);
        },
        onTransaction: ({ editor }) => this.refreshTools(editor),
      });
      this.refreshTools(this.editor);
    });
    effect(() => {
      const html = this.content();
      untracked(() => {
        if (this.editor && html !== this.current) {
          this.current = html;
          this.editor.commands.setContent(html, { emitUpdate: false });
        }
      });
    });
    inject(DestroyRef).onDestroy(() => this.editor?.destroy());
  }

  /** Inserts a placeholder at the cursor. */
  insert(token: string): void {
    this.editor?.chain().focus().insertContent(token + ' ').run();
  }

  protected apply(tool: Tool): void {
    if (this.editor) {
      tool.run(this.editor);
    }
  }

  private refreshTools(editor: Editor): void {
    this.activeTools.set(new Set(TOOLS.filter((t) => t.active?.(editor)).map((t) => t.id)));
  }
}
