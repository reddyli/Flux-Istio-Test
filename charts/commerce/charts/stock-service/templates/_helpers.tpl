{{/*
Named templates for stock-service.

Files starting with "_" are not rendered as K8s resources; they exist
only to define helpers (`{{ include "<name>" . }}`) consumed by the
real resource templates next to this file.
*/}}

{{/*
stock-service.name
The bare app name, used in labels. Defaults to the chart name; can be
overridden with `nameOverride` in values for the rare case where the
chart is reused with a different visible name.
*/}}
{{- define "stock-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
stock-service.fullname
Release-qualified resource name, e.g. "dev-stock-service". Truncated to
K8s' 63-char limit. The contains-check avoids the silly
"dev-dev-stock-service" when the release name already contains the
chart name.
*/}}
{{- define "stock-service.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
stock-service.selectorLabels
The stable subset of labels used in matchLabels selectors. These must
NOT include anything that changes between releases (no version, no
managed-by) or the selector will stop matching old pods on every
upgrade.
*/}}
{{- define "stock-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "stock-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
stock-service.labels
Every recommended label, applied to metadata.labels on every resource.
Includes the selector labels plus version / managed-by / part-of.
*/}}
{{- define "stock-service.labels" -}}
{{ include "stock-service.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Chart.Name }}
{{- end -}}
