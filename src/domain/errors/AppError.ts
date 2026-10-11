export interface ErrorDetail {
  field?: string;
  issue: string;
}

export class AppError extends Error {
  public readonly errorCode: string;
  public readonly statusCode: number;
  public readonly details: ErrorDetail[];

  constructor(errorCode: string, message: string, statusCode = 400, details: ErrorDetail[] = []) {
    super(message);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
    this.details = details;
    Object.setPrototypeOf(this, AppError.prototype);
  }
}
