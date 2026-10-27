import { commonEn } from './common.i18n';
import { homeEn } from '../features/home/home.i18n';
import { shellEn } from '../shell/shell.i18n';
import { loginEn } from '../features/login/login.i18n';
import type { Messages } from './messages.fr';

/** English texts: typed on the French ones, a missing text does not compile. */
export const en: Messages = {
  common: commonEn,
  shell: shellEn,
  login: loginEn,
  home: homeEn,
};
