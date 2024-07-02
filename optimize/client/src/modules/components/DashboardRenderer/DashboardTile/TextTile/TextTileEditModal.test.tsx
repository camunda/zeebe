/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {shallow} from 'enzyme';
import {SerializedEditorState, SerializedLexicalNode} from 'lexical';

import TextTileEditModal from './TextTileEditModal';

const editorValue: SerializedEditorState = {
  root: {
    children: [
      {
        children: [
          {
            detail: 0,
            format: 0,
            mode: 'normal',
            style: '',
            text: 'some text',
            type: 'text',
            version: 1,
          },
        ],
        direction: 'ltr',
        format: '',
        indent: 0,
        type: 'paragraph',
        version: 1,
      } as SerializedLexicalNode,
    ],
    direction: 'ltr',
    format: '',
    indent: 0,
    type: 'root',
    version: 1,
  },
};

const props = {
  initialValue: editorValue,
  onClose: jest.fn(),
  onConfirm: jest.fn(),
};

it('should render the modal', () => {
  const node = shallow(<TextTileEditModal {...props} />);

  const editor = node.find('TextEditor');

  expect(editor).toExist();
  expect(editor.prop('initialValue')).toEqual(editorValue);
});

it('should disable the submit button if the text in editor is empty or too long', () => {
  const node = shallow(
    <TextTileEditModal {...props} initialValue={{root: {...editorValue.root, children: []}}} />
  );

  expect(node.find('Button').at(1)).toBeDisabled();

  const normalText = {
    root: {
      children: [
        {
          children: [
            {
              text: 'a'.repeat(10),
            },
          ],
          type: 'paragraph',
        },
      ],
      type: 'root',
    },
  };
  node.find('TextEditor').simulate('change', normalText);
  expect(node.find('Button').at(1)).not.toBeDisabled();

  const tooLongText = {
    root: {
      children: [
        {
          children: [
            {
              text: 'a'.repeat(3001),
            },
          ],
          type: 'paragraph',
        },
      ],
      type: 'root',
    },
  };
  node.find('TextEditor').simulate('change', tooLongText);

  expect(node.find('Button').at(1)).toBeDisabled();
});

it('should invoke onConfirm when the submit button is clicked', () => {
  const spy = jest.fn();
  const node = shallow(<TextTileEditModal {...props} initialValue={editorValue} onConfirm={spy} />);

  const newEditorValue = {
    root: {
      children: [
        {
          children: [
            {
              detail: 0,
              format: 0,
              mode: 'normal',
              style: '',
              text: 'some new text',
              type: 'text',
              version: 1,
            },
          ],
          direction: 'ltr',
          format: '',
          indent: 0,
          type: 'paragraph',
          version: 1,
        },
      ],
      direction: 'ltr',
      format: '',
      indent: 0,
      type: 'root',
      version: 1,
    },
  };

  node.find('TextEditor').simulate('change', newEditorValue);

  node.find('Button').at(1).simulate('click');

  expect(spy).toHaveBeenCalledWith(newEditorValue);
});
