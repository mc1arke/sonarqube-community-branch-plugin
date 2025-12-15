/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import { Label, Select, TextInput } from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { RadioButton } from '~design-system';
import { NUMBER_OF_DAYS_MAX_VALUE, NUMBER_OF_DAYS_MIN_VALUE } from '~sq-server-commons/helpers/new-code-definition';
import { NewCodeDefinitionType } from '~sq-server-commons/types/new-code-definition';
import { NewCodeDefinitionLevels } from '~sq-server-commons/components/new-code-definition/utils';

interface BaseOptionProps {
  onSelect: (type: NewCodeDefinitionType) => void;
  selected: boolean;
}

interface NewCodeDefinitionDaysOptionProps extends BaseOptionProps {
  days: string;
  isValid: boolean;
  onChangeDays: (days: string) => void;
  settingLevel: NewCodeDefinitionLevels;
}

interface NewCodeDefinitionPreviousVersionOptionProps extends BaseOptionProps {
  isDefault: boolean;
}

interface NewCodeDefinitionSettingReferenceBranchProps extends BaseOptionProps {
  branchList: Array<{ label: string; value: string; isMain?: boolean; isDisabled?: boolean }>;
  onChangeReferenceBranch: (branch: string) => void;
  referenceBranch: string;
  settingLevel: NewCodeDefinitionLevels;
  inputSelectMenuPlacement?: string;
}

interface NewCodeDefinitionSettingAnalysisProps extends BaseOptionProps {
  analysis: string;
  branch: string;
  component: string;
}

export function NewCodeDefinitionPreviousVersionOption({
  isDefault: _isDefault,
  onSelect,
  selected,
}: Readonly<NewCodeDefinitionPreviousVersionOptionProps>) {
  const intl = useIntl();
  
  return (
    <div className="sw-mb-4 sw-pb-4 sw-border-b">
      <RadioButton
        checked={selected}
        onCheck={() => onSelect(NewCodeDefinitionType.PreviousVersion)}
        value={NewCodeDefinitionType.PreviousVersion}
      >
        <div>
          <div className="sw-mb-2">
            {intl.formatMessage({ id: 'new_code_definition.specific_setting.previous_version.label' })}
          </div>
          <div className="sw-ml-4 sw-text-sm sw-text-gray-600">
            <FormattedMessage id="new_code_definition.specific_setting.previous_version.description" />
          </div>
        </div>
      </RadioButton>
    </div>
  );
}

export function NewCodeDefinitionDaysOption({
  days,
  isValid,
  onChangeDays,
  onSelect,
  selected,
  settingLevel: _settingLevel,
}: Readonly<NewCodeDefinitionDaysOptionProps>) {
  const intl = useIntl();
  
  return (
    <div className="sw-mb-4 sw-pb-4 sw-border-b">
      <RadioButton
        checked={selected}
        onCheck={() => onSelect(NewCodeDefinitionType.NumberOfDays)}
        value={NewCodeDefinitionType.NumberOfDays}
      >
        <div>
          <div className="sw-mb-2">
            {intl.formatMessage({ id: 'new_code_definition.specific_setting.number_of_days.label' })}
          </div>
          <div className="sw-ml-4 sw-text-sm sw-text-gray-600">
            <FormattedMessage id="new_code_definition.specific_setting.number_of_days.description" />
          </div>
          {selected && (
            <div className="sw-ml-4 sw-mt-3">
              <TextInput
                isRequired
                label={
                  <Label>
                    <FormattedMessage id="new_code_definition.specific_setting.number_of_days.input.label" />
                  </Label>
                }
                max={NUMBER_OF_DAYS_MAX_VALUE}
                messageInvalid={
                  <FormattedMessage
                    id="new_code_definition.specific_setting.number_of_days.input.error"
                    values={{ min: NUMBER_OF_DAYS_MIN_VALUE, max: NUMBER_OF_DAYS_MAX_VALUE }}
                  />
                }
                min={NUMBER_OF_DAYS_MIN_VALUE}
                onChange={(event) => onChangeDays(event.target.value)}
                type="number"
                validation={isValid ? 'none' : 'invalid'}
                value={days}
                width="large"
              />
            </div>
          )}
        </div>
      </RadioButton>
    </div>
  );
}

export function NewCodeDefinitionSettingReferenceBranch({
  branchList,
  onChangeReferenceBranch,
  onSelect,
  referenceBranch,
  selected,
  settingLevel,
  inputSelectMenuPlacement: _inputSelectMenuPlacement,
}: Readonly<NewCodeDefinitionSettingReferenceBranchProps>) {
  const intl = useIntl();
  
  return (
    <div className="sw-mb-4 sw-pb-4 sw-border-b">
      <RadioButton
        checked={selected}
        onCheck={() => onSelect(NewCodeDefinitionType.ReferenceBranch)}
        value={NewCodeDefinitionType.ReferenceBranch}
      >
        <div>
          <div className="sw-mb-2">
            {intl.formatMessage({ id: 'new_code_definition.specific_setting.reference_branch.label' })}
          </div>
          <div className="sw-ml-4 sw-text-sm sw-text-gray-600">
            <FormattedMessage id="new_code_definition.specific_setting.reference_branch.description" />
          </div>
          {selected && (
            <div className="sw-ml-4 sw-mt-3">
              <Select
                data={branchList.filter((b) => !b.isDisabled).map((b) => ({
                  label: b.label,
                  value: b.value,
                }))}
                helpText={
                  settingLevel !== NewCodeDefinitionLevels.Branch ? (
                    <FormattedMessage id="new_code_definition.specific_setting.reference_branch.input.help.main" />
                  ) : null
                }
                isRequired
                label={
                  <Label>
                    <FormattedMessage id="new_code_definition.specific_setting.reference_branch.input.label" />
                  </Label>
                }
                onChange={(val) => onChangeReferenceBranch(val ?? '')}
                value={referenceBranch}
                width="large"
              />
            </div>
          )}
        </div>
      </RadioButton>
    </div>
  );
}

export function NewCodeDefinitionSettingAnalysis({
  analysis: _analysis,
  branch: _branch,
  component: _component,
  onSelect,
  selected,
}: Readonly<NewCodeDefinitionSettingAnalysisProps>) {
  const intl = useIntl();
  
  return (
    <div className="sw-mb-4">
      <RadioButton
        checked={selected}
        disabled
        onCheck={() => onSelect(NewCodeDefinitionType.SpecificAnalysis)}
        value={NewCodeDefinitionType.SpecificAnalysis}
      >
        <div>
          <div className="sw-mb-2">
            {intl.formatMessage({ id: 'new_code_definition.specific_setting.specific_analysis.label' })}
          </div>
          <div className="sw-ml-4 sw-text-sm sw-text-gray-600">
            <FormattedMessage id="new_code_definition.specific_setting.specific_analysis.description" />
          </div>
        </div>
      </RadioButton>
    </div>
  );
}
