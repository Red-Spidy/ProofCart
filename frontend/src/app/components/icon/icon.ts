import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';

/**
 * Single shared icon set for the whole app (Feather-style outline strokes) so nothing renders
 * as a platform-dependent emoji glyph. Add a new <path>/<svg> case here rather than reaching
 * for an emoji in a template.
 */
@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="strokeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true">
      <ng-container [ngSwitch]="name">
        <ng-container *ngSwitchCase="'shield-check'">
          <path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z"/>
          <path d="M9 12l2 2 4-4"/>
        </ng-container>
        <ng-container *ngSwitchCase="'home'">
          <path d="M4 11l8-7 8 7"/>
          <path d="M6 10v9a1 1 0 0 0 1 1h4v-6h2v6h4a1 1 0 0 0 1-1v-9"/>
        </ng-container>
        <ng-container *ngSwitchCase="'package'">
          <path d="M21 8l-9-5-9 5 9 5 9-5z"/>
          <path d="M3 8v8l9 5 9-5V8"/>
          <path d="M12 13v8"/>
        </ng-container>
        <ng-container *ngSwitchCase="'store'">
          <path d="M4 9l1-5h14l1 5"/>
          <path d="M3 9h18v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2 2 2 2 0 0 1-2-2V9z"/>
          <path d="M5 13v7h14v-7"/>
          <path d="M10 20v-5h4v5"/>
        </ng-container>
        <ng-container *ngSwitchCase="'cart'">
          <circle cx="9" cy="20" r="1.4"/>
          <circle cx="18" cy="20" r="1.4"/>
          <path d="M2.5 3h2.4l1.9 11.2a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 2-1.6L21 7H6.2"/>
        </ng-container>
        <ng-container *ngSwitchCase="'check'">
          <path d="M20 6L9 17l-5-5"/>
        </ng-container>
        <ng-container *ngSwitchCase="'x'">
          <path d="M18 6L6 18M6 6l12 12"/>
        </ng-container>
        <ng-container *ngSwitchCase="'alert-triangle'">
          <path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/>
          <path d="M12 9v4"/>
          <path d="M12 17h.01"/>
        </ng-container>
        <ng-container *ngSwitchCase="'lock'">
          <rect x="4" y="11" width="16" height="9" rx="2"/>
          <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
        </ng-container>
        <ng-container *ngSwitchCase="'unlock'">
          <rect x="4" y="11" width="16" height="9" rx="2"/>
          <path d="M8 11V7a4 4 0 0 1 7.3-2.2"/>
        </ng-container>
        <ng-container *ngSwitchCase="'chevron-right'">
          <path d="M9 6l6 6-6 6"/>
        </ng-container>
        <ng-container *ngSwitchCase="'arrow-left'">
          <path d="M19 12H5"/>
          <path d="M12 19l-7-7 7-7"/>
        </ng-container>
        <ng-container *ngSwitchCase="'plus'">
          <path d="M12 5v14M5 12h14"/>
        </ng-container>
        <ng-container *ngSwitchCase="'minus'">
          <path d="M5 12h14"/>
        </ng-container>
        <ng-container *ngSwitchCase="'thumbs-up'">
          <path d="M7 22V11l5-8 1.5 1.5c.4.4.6 1 .5 1.5L13 10h6.5a2 2 0 0 1 2 2.3l-1.2 8A2 2 0 0 1 18.3 22H7z"/>
          <path d="M7 11H3v11h4"/>
        </ng-container>
        <ng-container *ngSwitchCase="'thumbs-down'">
          <path d="M17 2v11l-5 8-1.5-1.5c-.4-.4-.6-1-.5-1.5l1-4H4.5a2 2 0 0 1-2-2.3l1.2-8A2 2 0 0 1 5.7 2H17z"/>
          <path d="M17 13h4V2h-4"/>
        </ng-container>
        <ng-container *ngSwitchCase="'search'">
          <circle cx="11" cy="11" r="7"/>
          <path d="M21 21l-4.35-4.35"/>
        </ng-container>
        <ng-container *ngSwitchCase="'zap'">
          <path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z"/>
        </ng-container>
        <ng-container *ngSwitchCase="'truck'">
          <rect x="1" y="7" width="14" height="10" rx="1"/>
          <path d="M15 10h4l3 3.5V17h-7"/>
          <circle cx="5.5" cy="18.5" r="1.6"/>
          <circle cx="17.5" cy="18.5" r="1.6"/>
        </ng-container>
        <ng-container *ngSwitchCase="'log-out'">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <path d="M16 17l5-5-5-5"/>
          <path d="M21 12H9"/>
        </ng-container>
        <ng-container *ngSwitchCase="'mail'">
          <rect x="2.5" y="4.5" width="19" height="15" rx="2"/>
          <path d="M3 6l9 7 9-7"/>
        </ng-container>
        <ng-container *ngSwitchCase="'key'">
          <circle cx="8" cy="15" r="4.5"/>
          <path d="M11.5 11.5L21 2"/>
          <path d="M16 7l3 3"/>
          <path d="M19 4l3 3"/>
        </ng-container>
        <ng-container *ngSwitchCase="'loader'">
          <path d="M12 3v3"/>
          <path d="M12 18v3"/>
          <path d="M5.6 5.6l2.1 2.1"/>
          <path d="M16.3 16.3l2.1 2.1"/>
          <path d="M3 12h3"/>
          <path d="M18 12h3"/>
          <path d="M5.6 18.4l2.1-2.1"/>
          <path d="M16.3 7.7l2.1-2.1"/>
        </ng-container>
        <ng-container *ngSwitchCase="'sparkles'">
          <path d="M12 3l1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6L12 3z"/>
          <path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z"/>
        </ng-container>
        <ng-container *ngSwitchCase="'receipt'">
          <path d="M6 2h12v19l-3-1.5L12 21l-3-1.5L6 21V2z"/>
          <path d="M9 7h6M9 11h6"/>
        </ng-container>
        <ng-container *ngSwitchCase="'layers'">
          <path d="M12 2l9 5-9 5-9-5 9-5z"/>
          <path d="M3 12l9 5 9-5"/>
          <path d="M3 17l9 5 9-5"/>
        </ng-container>
        <ng-container *ngSwitchCase="'user'">
          <circle cx="12" cy="8" r="4"/>
          <path d="M4 21a8 8 0 0 1 16 0"/>
        </ng-container>
        <ng-container *ngSwitchCase="'clock'">
          <circle cx="12" cy="12" r="9"/>
          <path d="M12 7v5l3.5 2"/>
        </ng-container>
        <ng-container *ngSwitchCase="'trash'">
          <path d="M4 7h16"/>
          <path d="M9 7V4h6v3"/>
          <path d="M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/>
        </ng-container>
        <ng-container *ngSwitchCase="'shield'">
          <path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z"/>
        </ng-container>
      </ng-container>
    </svg>
  `,
})
export class IconComponent {
  @Input() name = '';
  @Input() size = 18;
  @Input() strokeWidth = 2;
}
