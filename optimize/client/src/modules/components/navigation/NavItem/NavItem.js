/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import classnames from 'classnames';
import {Link, withRouter} from 'react-router-dom';
import {matchPath} from 'react-router';

import {Tooltip} from 'components';

import {loadEntitiesNames} from './service';

import './NavItem.scss';

const breadcrumbConstructors = [];

export default withRouter(
  class NavItem extends React.Component {
    state = {
      breadcrumbs: [],
    };

    async componentDidMount() {
      await this.constructBreadcrumbs();
      breadcrumbConstructors.push(this.constructBreadcrumbs);
    }

    componentWillUnmount() {
      breadcrumbConstructors.splice(breadcrumbConstructors.indexOf(this.constructBreadcrumbs), 1);
    }

    async componentDidUpdate(prevProps) {
      if (prevProps.location.pathname !== this.props.location.pathname) {
        await this.constructBreadcrumbs();
      }
    }

    constructBreadcrumbs = async () => {
      const {
        location: {pathname},
        breadcrumbsEntities,
      } = this.props;

      if (!breadcrumbsEntities?.length || !this.isActive()) {
        return;
      }

      let breadcrumbs = [];
      const entitiesIds = {};
      breadcrumbsEntities.forEach(({entity, entityUrl = entity}) => {
        const [baseUrl, paramUrl] = pathname.split(`/${entityUrl}/`);
        if (paramUrl) {
          const id = paramUrl.split('/')[0];
          if (!['new', 'new-combined', 'new-decision', 'instant'].includes(id)) {
            entitiesIds[entity + 'Id'] = id;
            breadcrumbs.push({
              id,
              type: entity,
              url: baseUrl + `/${entityUrl}/${id}/`,
            });
          }
        }
      });

      if (breadcrumbs.length > 0) {
        try {
          const names = await loadEntitiesNames(entitiesIds);
          breadcrumbs = breadcrumbs
            .map((breadcrumb) => ({
              ...breadcrumb,
              name: names[breadcrumb.type + 'Name'],
            }))
            .filter((breadcrumb) => breadcrumb.name);
        } catch (error) {
          breadcrumbs = [];
        }
      }

      this.setState({breadcrumbs});
    };

    isActive = () =>
      matchPath(this.props.location.pathname, {path: this.props.active, exact: true}) !== null;

    render() {
      const {name, linksTo} = this.props;
      const {breadcrumbs} = this.state;
      const breadcrumbsCount = breadcrumbs.length;

      const active = this.isActive();

      return (
        <div className="NavItem">
          <Tooltip content={name} position="bottom" overflowOnly>
            <Link
              to={linksTo}
              className={classnames('cds--header__menu-item', {
                'cds--header__menu-item--current': active && !breadcrumbsCount,
              })}
              replace={active}
            >
              {name}
            </Link>
          </Tooltip>
          {active &&
            breadcrumbs.map(({id, name, url}, i) => (
              <Tooltip content={name} key={`${id}_${url}`} position="bottom" overflowOnly>
                <span className="arrow">›</span>
                <Link
                  className={classnames('breadcrumb cds--header__menu-item', {
                    'cds--header__menu-item--current': breadcrumbsCount - 1 === i,
                  })}
                  key={id}
                  to={url}
                >
                  {name}
                </Link>
              </Tooltip>
            ))}
        </div>
      );
    }
  }
);

export function refreshBreadcrumbs() {
  breadcrumbConstructors.forEach((fct) => fct());
}
