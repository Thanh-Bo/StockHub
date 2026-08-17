import { Component, Input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import type { LucideIcon } from '@lucide/angular';
import {
  LucideActivity,
  LucideAlertCircle,
  LucideAlertTriangle,
  LucideArrowDown,
  LucideArrowLeft,
  LucideArrowLeftRight,
  LucideArrowRight,
  LucideArrowUp,
  LucideBarChart3,
  LucideBookmark,
  LucideBuilding2,
  LucideCalendar,
  LucideCheck,
  LucideChevronDown,
  LucideChevronLeft,
  LucideChevronUp,
  LucideCircleUserRound,
  LucideDollarSign,
  LucideEye,
  LucideEyeOff,
  LucideFilter,
  LucideHelpCircle,
  LucideInfo,
  LucideLandmark,
  LucideLoader2,
  LucideLock,
  LucideLogOut,
  LucideMail,
  LucideMenu,
  LucideMinus,
  LucideMoreHorizontal,
  LucidePencil,
  LucidePercent,
  LucidePlus,
  LucideRefreshCw,
  LucideSearch,
  LucideSearchX,
  LucideSettings,
  LucideShieldCheck,
  LucideSlidersHorizontal,
  LucideStar,
  LucideTrash2,
  LucideTrendingDown,
  LucideTrendingUp,
  LucideUser,
  LucideUsers,
  LucideWallet,
  LucideX,
} from '@lucide/angular';

const ICONS: Record<string, LucideIcon> = {
  trending_up: LucideTrendingUp,
  trending_down: LucideTrendingDown,
  trending_flat: LucideMinus,
  search: LucideSearch,
  search_off: LucideSearchX,
  filter_list: LucideSlidersHorizontal,
  tune: LucideSlidersHorizontal,
  filter: LucideFilter,
  compare: LucideArrowLeftRight,
  compare_arrows: LucideArrowLeftRight,
  bookmark: LucideBookmark,
  account_circle: LucideCircleUserRound,
  arrow_drop_down: LucideChevronDown,
  arrow_drop_up: LucideChevronUp,
  arrow_back: LucideArrowLeft,
  arrow_right: LucideArrowRight,
  chevron_left: LucideChevronLeft,
  logout: LucideLogOut,
  mail: LucideMail,
  person: LucideUser,
  person_outline: LucideUser,
  lock: LucideLock,
  visibility: LucideEye,
  visibility_off: LucideEyeOff,
  add: LucidePlus,
  edit: LucidePencil,
  check: LucideCheck,
  close: LucideX,
  delete_outline: LucideTrash2,
  error_outline: LucideAlertCircle,
  error: LucideAlertCircle,
  warning: LucideAlertTriangle,
  info: LucideInfo,
  help: LucideHelpCircle,
  spinner: LucideLoader2,
  star: LucideStar,
  calendar: LucideCalendar,
  dollar: LucideDollarSign,
  percent: LucidePercent,
  landmark: LucideLandmark,
  refresh: LucideRefreshCw,
  settings: LucideSettings,
  menu: LucideMenu,
  more: LucideMoreHorizontal,
  arrow_upward: LucideArrowUp,
  arrow_downward: LucideArrowDown,
  chart: LucideBarChart3,
  users: LucideUsers,
  building: LucideBuilding2,
  wallet: LucideWallet,
  activity: LucideActivity,
  shield: LucideShieldCheck,
};

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [LucideDynamicIcon],
  template: `
    <svg
      [lucideIcon]="icon"
      [size]="size"
      [strokeWidth]="strokeWidth"
      class="inline-block shrink-0"
    ></svg>
  `,
})
export class AppIconComponent {
  @Input() name = 'help';
  @Input() size = 16;
  @Input() strokeWidth = 2;

  get icon(): LucideIcon {
    return ICONS[this.name] ?? LucideHelpCircle;
  }
}
