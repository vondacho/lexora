import { commonFr } from './common.i18n';
import { homeFr } from '../features/home/home.i18n';
import { shellFr } from '../shell/shell.i18n';
import { loginFr } from '../features/login/login.i18n';

/** French texts of the interface, one section per screen. */
export const fr = {
  common: commonFr,
  shell: shellFr,
  login: loginFr,
  home: homeFr,
};

export type Messages = typeof fr;
