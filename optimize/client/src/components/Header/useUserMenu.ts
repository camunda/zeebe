/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useEffect, useState} from 'react';
import {useHistory} from 'react-router-dom';
import {ArrowRight} from '@carbon/react/icons';
import {C3NavigationProps} from '@camunda/camunda-composite-components';

import {t} from 'translation';
import {isLogoutHidden} from 'config';
import {showError} from 'notifications';
import {useErrorHandling, useUser} from 'hooks';

export default function useUserMenu() {
  const [logoutHidden, setLogoutHidden] = useState(false);
  const history = useHistory();
  const {mightFail} = useErrorHandling();
  const {user} = useUser();

  useEffect(() => {
    mightFail(isLogoutHidden(), setLogoutHidden, showError);
  }, [mightFail, user]);

  const menu: Exclude<C3NavigationProps['userSideBar'], undefined> = {
    ariaLabel: t('common.user.label').toString(),
    customElements: {
      profile: {
        label: t('navigation.profile').toString(),
        user: {
          email: user?.email || '',
          name: user?.name || '',
        },
      },
    },
    elements: [
      {
        key: 'terms',
        label: t('navigation.termsOfUse').toString(),
        onClick: () => {
          window.open(
            'https://camunda.com/legal/terms/camunda-platform/camunda-platform-8-saas-trial/',
            '_blank'
          );
        },
      },
      {
        key: 'privacy',
        label: t('navigation.privacyPolicy').toString(),
        onClick: () => {
          window.open('https://camunda.com/legal/privacy/', '_blank');
        },
      },
      {
        key: 'imprint',
        label: t('navigation.imprint').toString(),
        onClick: () => {
          window.open('https://camunda.com/legal/imprint/', '_blank');
        },
      },
    ],
    bottomElements: [],
  };

  if (!logoutHidden) {
    menu.bottomElements?.push({
      key: 'logout',
      label: t('navigation.logout').toString(),
      kind: 'ghost',
      onClick: () => history.replace('/logout'),
      renderIcon: ArrowRight,
    });
  }

  return menu;
}
