export class ApiError extends Error {
  readonly status: number
  readonly currentStatus?: string

  constructor(status: number, detail: string, currentStatus?: string) {
    super(detail)
    this.status = status
    this.currentStatus = currentStatus
  }
}

/**
 * Thin fetch wrapper that attaches the caller's trusted identity headers
 * (X-Client-Id / X-Operator-Id, per research.md #1) to every request.
 */
export async function apiFetch<T>(
  path: string,
  identityHeaders: Record<string, string>,
  init: RequestInit = {},
): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...identityHeaders,
      ...(init.headers as Record<string, string> | undefined),
    },
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(
      response.status,
      problem?.detail ?? `Request failed with status ${response.status}`,
      problem?.currentStatus,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
