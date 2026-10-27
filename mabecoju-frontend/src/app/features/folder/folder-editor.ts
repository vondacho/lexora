import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, firstValueFrom, of } from 'rxjs';
import { Folder, FolderInput, FoldersService, FolderStatus } from '../../api/generated';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { cleaned, FolderFields } from '../folders/folder-fields';

/** "Modifier le dossier": its details, and its deletion with everything it holds. */
@Component({
  selector: 'mbj-folder-editor',
  imports: [FormsModule, FolderFields],
  template: `
    <section class="card composer" aria-labelledby="folder-edit-title">
      <div class="composer-head">
        <h2 id="folder-edit-title">{{ tx().folder.folderEditor.title }}</h2>
        <button type="button" class="btn btn-small" (click)="closed.emit()">{{ tx().common.close }}</button>
      </div>
      <form (ngSubmit)="save()">
        <mbj-folder-fields [value]="draft" [statuses]="statuses()" prefix="edit-folder" />
        @if (error(); as message) {
          <p class="problem" role="alert">{{ message }}</p>
        }
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="busy()">{{ busy() ? tx().common.saving : tx().common.save }}</button>
        </div>
      </form>
      @if (canDelete()) {
        <div class="danger">
          <span class="label">{{ tx().folder.folderEditor.deleteTitle }}</span>
          <p class="note muted">{{ tx().folder.folderEditor.deleteNote }}</p>
          @if (confirming()) {
            <div class="field">
              <label for="confirm-name">{{ tx().folder.folderEditor.typeToConfirm(folder().name ?? '') }}</label>
              <input id="confirm-name" class="input" autocomplete="off" [(ngModel)]="typed" />
            </div>
            <div class="actions">
              <button type="button" class="btn" (click)="confirming.set(false)">{{ tx().common.cancel }}</button>
              <button type="button" class="btn btn-warn" [disabled]="busy() || typed.trim() !== (folder().name ?? '').trim()" (click)="remove()">{{ tx().folder.folderEditor.deleteForever }}</button>
            </div>
          } @else {
            <button type="button" class="btn btn-warn" (click)="confirming.set(true)">{{ tx().folder.folderEditor.deleteStart }}</button>
          }
        </div>
      }
    </section>
  `,
  styleUrl: './composer.css',
  styles: `
    form { display: flex; flex-direction: column; gap: 12px; }
    .danger { display: flex; flex-direction: column; gap: 8px; padding-top: 12px; border-top: 1px solid var(--border); }
    .danger .btn-warn { align-self: flex-start; }
  `,
})
export class FolderEditor {
  private readonly folders = inject(FoldersService);

  readonly folder = input.required<Folder>();
  readonly canDelete = input(false);
  readonly saved = output<void>();
  readonly deleted = output<void>();
  readonly closed = output<void>();

  protected readonly tx = texts;
  protected draft: FolderInput = { type: 'INVOICING', name: '' };
  protected readonly statuses = signal<string[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirming = signal(false);
  protected typed = '';

  constructor() {
    effect(() => {
      const f = this.folder();
      this.draft = {
        type: f.type === 'TUTELAGE' ? 'TUTELAGE' : 'INVOICING',
        name: f.name ?? '',
        status: f.status,
        handledBy: f.handledBy,
        openingDate: f.openingDate,
        firstDate: f.firstDate,
        wayToOurOffice: f.wayToOurOffice,
        comment: f.comment,
        path: f.path,
      };
    });
    this.folders
      .listFolderStatuses()
      .pipe(catchError(() => of([] as FolderStatus[])))
      .subscribe((list) => this.statuses.set(list.map((s) => s.name ?? '').filter(Boolean)));
  }

  protected async save(): Promise<void> {
    if (!this.draft.name?.trim()) {
      this.error.set(texts().folder.folderEditor.nameMissing);
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.folders.updateFolder({ id: this.folder().id, folderInput: cleaned(this.draft) }));
      this.saved.emit();
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().folder.folderEditor.saveFailed));
    } finally {
      this.busy.set(false);
    }
  }

  protected async remove(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.folders.deleteFolder({ id: this.folder().id }), { defaultValue: null });
      this.deleted.emit();
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().folder.folderEditor.deleteFailed));
      this.busy.set(false);
    }
  }
}
