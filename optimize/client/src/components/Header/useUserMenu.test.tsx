/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {ComponentProps} from 'react';
import {runLastEffect} from '__mocks__/react';
import {useHistory} from 'react-router-dom';
import {shallow} from 'enzyme';

import {isLogoutHidden} from 'config';

import useUserMenu from './useUserMenu';

jest.mock('config', () => ({
  isLogoutHidden: jest.fn().mockReturnValue(false),
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useHistory: jest.fn(),
}));

jest.mock('hooks', () => ({
  useErrorHandling: jest
    .fn()
    .mockReturnValue({mightFail: jest.fn().mockImplementation((data, cb) => cb(data))}),
  useUser: jest.fn().mockReturnValue({user: {authorizations: [], name: 'userName'}}),
}));

const UserMenu = () => {
  const menu = useUserMenu();
  return <div {...(menu as ComponentProps<'div'>)} />;
};

it('should go to temporary logout route on logout', () => {
  const spy = {replace: jest.fn()};
  (useHistory as jest.Mock).mockReturnValue(spy);
  const node = shallow(<UserMenu />);

  const logout = node.props().bottomElements[0];
  logout.onClick();
  expect(spy.replace).toHaveBeenCalledWith('/logout');
});

it('should hide logout button if specified by the ui config', () => {
  (isLogoutHidden as jest.Mock).mockReturnValueOnce(true);
  const node = shallow(<UserMenu />);

  runLastEffect();

  const logout = node.props().bottomElements[0];
  expect(logout).not.toBeDefined();
});
