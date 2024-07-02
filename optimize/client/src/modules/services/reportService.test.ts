/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {isTextTileTooLong, isTextTileValid} from './reportService';

describe('isTextTileValid', () => {
  it('should return true if report is valid', () => {
    expect(isTextTileValid(100)).toBe(true);
  });

  it('should return false if report is not valid', () => {
    expect(isTextTileValid(0)).toBe(false);
    expect(isTextTileValid(3001)).toBe(false);
  });
});

describe('isTextTileTooLong', () => {
  it('should return true if report is too long', () => {
    expect(isTextTileTooLong(3001)).toBe(true);
    expect(isTextTileTooLong(3001, 100)).toBe(true);
  });

  it('should return false if report is not too long', () => {
    expect(isTextTileTooLong(100)).toBe(false);
    expect(isTextTileTooLong(100, 200)).toBe(false);
  });
});
