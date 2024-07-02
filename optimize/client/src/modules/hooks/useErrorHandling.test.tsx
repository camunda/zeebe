/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {runLastEffect, runLastCleanup} from '__mocks__/react';
import {shallow} from 'enzyme';

import useErrorHandling from './useErrorHandling';

function Mock({
  promise,
  onSuccess,
  onError,
  onFinally,
  resultHandler,
}: {
  promise: Promise<any>;
  onSuccess: (...args: any) => void;
  onError?: (...args: any) => void;
  onFinally?: (...args: any) => void;
  resultHandler?: (result: any) => void;
}) {
  const {mightFail, error, resetError} = useErrorHandling();
  return (
    <>
      <button
        onClick={async () => {
          const result = await mightFail(promise, onSuccess, onError, onFinally);
          resultHandler?.(result);
        }}
      />
      {error && (
        <div className="error">
          {error.toString()}
          <button className="errorBtn" onClick={() => resetError?.()} />
        </div>
      )}
    </>
  );
}

it('should pass the value of the receiver function to the callback', async () => {
  const spy = jest.fn();
  const node = shallow(<Mock promise={(async () => 32)()} onSuccess={spy} />);
  runLastEffect();

  node.find('button').simulate('click');
  await node.update();

  expect(spy).toHaveBeenCalledWith(32);
});

it('should return the value of the callback function from the mightfail', async () => {
  const spy = jest.fn();
  const node = shallow(
    <Mock promise={(async () => 32)()} onSuccess={(value) => value} resultHandler={spy} />
  );
  runLastEffect();

  node.find('button').simulate('click');
  await node.update();
  await flushPromises();

  expect(spy).toHaveBeenCalledWith(32);
});

it('should not pass the value of the receiver function to the function when component is unmountd', async () => {
  const spy = jest.fn();
  const node = shallow(<Mock promise={(async () => 32)()} onSuccess={spy} />);
  runLastEffect();
  runLastCleanup();

  node.find('button').simulate('click');
  await node.update();

  expect(spy).not.toHaveBeenCalled();
});

it('should catch errors', () => {
  const node = shallow(
    <Mock
      promise={(async () => {
        throw new Error();
      })()}
      onSuccess={jest.fn()}
    />
  );
  runLastEffect();
  node.find('button').simulate('click');
});

it('should pass an error and reset it via props', async () => {
  const error = new Error();
  const node = shallow(<Mock promise={Promise.reject(error)} onSuccess={jest.fn()} />);

  runLastEffect();
  node.find('button').simulate('click');
  await flushPromises();

  expect(node.find('.error')).toIncludeText(error.toString());

  node.find('.errorBtn').simulate('click');
  expect(node.find('.error')).not.toIncludeText(error.toString());
});

it('should call a custom error handler', async () => {
  const spy = jest.fn();
  const error = new Error();
  const node = shallow(
    <Mock promise={Promise.reject(error)} onSuccess={jest.fn()} onError={spy} />
  );

  runLastEffect();
  node.find('button').simulate('click');
  await node.update();

  expect(spy).toHaveBeenCalledWith(error);
});

it('should call finally after success', async () => {
  const spy = jest.fn();
  const node = shallow(
    <Mock promise={Promise.resolve(true)} onSuccess={jest.fn()} onFinally={spy} />
  );

  runLastEffect();
  node.find('button').simulate('click');
  await flushPromises();

  expect(spy).toHaveBeenCalled();
});

it('should call finally after error', async () => {
  const spy = jest.fn();
  const node = shallow(
    <Mock promise={Promise.reject(new Error())} onSuccess={jest.fn()} onFinally={spy} />
  );
  runLastEffect();
  node.find('button').simulate('click');
  await node.update();

  expect(spy).toHaveBeenCalled();
});
