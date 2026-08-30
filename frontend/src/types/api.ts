export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly errorTitle: string;
  readonly originalMessage: string;

  constructor(status: number, errorTitle: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorTitle = errorTitle;
    this.originalMessage = message;
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}

/**
 * Sanitizes backend or network error messages into human-friendly, professional copy.
 * Ensures raw exceptions, NullPointerExceptions, or stack traces are never shown to users.
 */
export function getHumanReadableErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    // Standardize well-known status codes and error messages
    if (error.status === 401) {
      return 'Invalid email or password. Please verify your credentials and try again.';
    }
    if (error.status === 409) {
      return 'An account with this email address already exists. Please log in instead.';
    }
    if (error.status === 403) {
      return 'You do not have permission to perform this action.';
    }
    if (error.status === 400) {
      // If validation error like "email: Merchant email must be valid", clean up field prefix if helpful
      if (error.originalMessage && !error.originalMessage.includes('Exception')) {
        return error.originalMessage;
      }
      return 'Please verify the submitted information and try again.';
    }
    if (error.status === 404) {
      return 'The requested resource could not be found.';
    }
    if (error.status >= 500) {
      return 'RecoverAI service encountered a temporary issue. Please try again in a moment.';
    }
    if (error.originalMessage && !error.originalMessage.includes('Exception')) {
      return error.originalMessage;
    }
  }

  if (error instanceof Error) {
    if (error.message.toLowerCase().includes('failed to fetch') || error.message.toLowerCase().includes('networkerror')) {
      return 'Unable to connect to RecoverAI service. Please check your internet connection or try again shortly.';
    }
    if (!error.message.includes('Exception') && !error.message.includes('undefined')) {
      return error.message;
    }
  }

  return 'An unexpected error occurred. Please try again later.';
}
