import styled from 'styled-components';
import {Colors, themed, themeStyle} from 'modules/theme';
import BadgeComponent from 'modules/components/Badge';

export const HEADER_HEIGHT = 56;
const separator = themeStyle({
  dark: 'rgba(246, 252, 251, 0.5)',
  light: 'rgba(98, 98, 110, 0.25)'
});

export const Dashboard = themed(styled.div`
  display: inline-block;
  padding: 0 20px;
  border-right: 1px solid ${separator};

  svg {
    width: 15px;
    height: 15px;
    margin-right: 20px;
    vertical-align: text-bottom;
    ${({active}) => (active ? '' : `opacity: 0.8`)};
  }

  span {
    ${({active}) => (active ? '' : `opacity: 0.5;`)};
  }
`);

export const Header = themed(styled.header`
  height: ${HEADER_HEIGHT}px;
  background-color: ${themeStyle({
    dark: Colors.uiDark01,
    light: Colors.uiLight01
  })};
  padding: 9px 0 0 0;
  font-size: 15px;
  font-weight: 500;
  color: ${themeStyle({
    dark: '#ffffff',
    light: Colors.uiLight06
  })};
  line-height: 19px;
  & > span {
    display: inline-block;
  }

  /* prevents user dropdown for going under content */
  /* each page content, with display: flex; should have a smaller z-index */
  position: relative;
  z-index: 2;
`);

export const ListLink = themed(styled.span`
  margin-left: 20px;
  & span {
    ${({active}) => (active ? '' : `opacity: 0.5;`)};
  }
`);

export const Badge = styled(BadgeComponent)`
  opacity: 0.8;
`;

export const Detail = themed(styled.span`
  padding-left: 20px;
  margin-left: 20px;
  border-left: 1px solid ${separator};
`);

export const ProfileDropdown = styled.span`
  margin-right: 20px;
  float: right;
  opacity: 0.9;
`;
