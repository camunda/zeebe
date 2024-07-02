/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {NavItem} from '../NavItem';

import './SubNav.scss';

export default function SubNav(props) {
  return (
    <ul role="navigation" className="SubNav">
      {props.children}
    </ul>
  );
}

SubNav.Item = function SubNavItem(props) {
  return (
    <li>
      <NavItem {...props} />
    </li>
  );
};
