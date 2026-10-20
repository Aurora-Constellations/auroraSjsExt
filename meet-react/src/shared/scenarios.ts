import type { Scenario } from './types';

const gcsReference = `Clinical:
Neurologic:
gcs_eye
gcs_verbal
gcs_motor`;

export const scenarios: Scenario[] = [
  {
    id: 'gcs-reference', label: 'GCS: complete designer',
    description: 'B contains all three required GCS items plus an extra pupil response.',
    left: gcsReference,
    right: `Clinical:
Neurologic:
gcs_eye [4 _]
gcs_verbal [2 _]
gcs_motor [6 _]
pupil_response [brisk _]`
  },
  {
    id: 'gcs-missing', label: 'GCS: missing response',
    description: 'B is missing the required motor response.',
    left: gcsReference,
    right: `Clinical:
Neurologic:
gcs_eye [4 _]
gcs_verbal [2 _]`
  },
  {
    id: 'gcs-composed', label: 'GCS: facts across files',
    description: 'The GCS inputs are split between A and B. Their join supplies a total of 7.',
    left: `Clinical:
Neurologic:
gcs_eye [2 _]
gcs_verbal [2 _]`,
    right: `Clinical:
Neurologic:
gcs_motor [3 _]
Orders:
Monitoring:
neurologic_observations`
  },
  {
    id: 'af', label: 'AF: stroke risk',
    description: 'Demographics in A and issues in B produce a CHA2DS2-VASc total of 5.',
    left: `Clinical:
Demographics:
age [76 yr]
sex [female _]
cha2ds2_vasc_diabetes [absent _]
prior_stroke_tia_te [absent _]
vascular_disease [absent _]`,
    right: `Issues:
atrial_fibrillation
heart_failure
hypertension`
  },
  {
    id: 'conflicting-values', label: 'Different values',
    description: 'The eye-response values differ. Join keeps both; meet retains the item without either value.',
    left: `Clinical:
Neurologic:
gcs_eye [2 _]
gcs_verbal [2 _]
gcs_motor [3 _]`,
    right: `Clinical:
Neurologic:
gcs_eye [4 _]
gcs_verbal [2 _]
gcs_motor [3 _]`
  },
  {
    id: 'wrong-group', label: 'Wrong group',
    description: 'B uses the same item names in another group, so reference validation fails.',
    left: gcsReference,
    right: `Clinical:
Observation:
gcs_eye [4 _]
gcs_verbal [5 _]
gcs_motor [6 _]`
  },
  {
    id: 'disjoint', label: 'Disjoint sections',
    description: 'A has Clinical and B has Orders. Their meet is empty; both sections reconstruct their join.',
    left: `Clinical:
Vitals:
temperature [37 C]`,
    right: `Orders:
Care:
repeat_observations`
  },
  {
    id: 'empty', label: 'Empty model',
    description: 'A is empty. Join preserves B, meet is empty, and reference validation passes.',
    left: '',
    right: `Clinical:
Neurologic:
gcs_eye [4 _]
gcs_verbal [5 _]
gcs_motor [6 _]`
  }
];
