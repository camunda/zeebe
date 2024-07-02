/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {shallow} from 'enzyme';
import CopyModal from './CopyModal';

const props = {
  entity: {name: 'Test Dashboard', entityType: 'dashboard', data: {subEntityCounts: {report: 2}}},
  collection: 'aCollectionId',
  onConfirm: jest.fn(),
  onClose: jest.fn(),
};

it('should render properly', () => {
  const node = shallow(<CopyModal {...props} />);

  expect(node.find('TextInput').prop('labelText')).toBe('Name of copy');
  expect(node.find('MoveCopy')).toExist();
});

it('should hide option to move the copy for collection entities', () => {
  const node = shallow(
    <CopyModal {...props} entity={{name: 'collection', entityType: 'collection'}} />
  );

  expect(node.find('MoveCopy')).not.toExist();
});

it('should call the onConfirm action', () => {
  const node = shallow(
    <CopyModal {...props} jumpToEntity entity={{name: 'collection', entityType: 'collection'}} />
  );

  node.find('.confirm').simulate('click');

  expect(node.find('Checkbox')).toExist();
  expect(props.onConfirm).toHaveBeenCalledWith('collection (copy)', true);
});

it('should hide the jump checkbox if jumpToEntity property is not added', () => {
  const node = shallow(<CopyModal {...props} />);

  expect(node.find('Checkbox')).not.toExist();

  node.find('.confirm').simulate('click');

  expect(props.onConfirm).toHaveBeenCalledWith('Test Dashboard (copy)', false, false);
});

it('should call the onConfirm with false redirect parameter if entity is not a collection', () => {
  const node = shallow(<CopyModal {...props} jumpToEntity />);

  node.find('.confirm').simulate('click');

  expect(props.onConfirm).toHaveBeenCalledWith('Test Dashboard (copy)', false, false);
});
