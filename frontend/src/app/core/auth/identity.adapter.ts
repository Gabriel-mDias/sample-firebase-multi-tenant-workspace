import { InjectionToken } from '@angular/core';
export interface IdentityUser {
  readonly uid: string;
  getIdToken(): Promise<string>;
}
export interface IdentityAdapter {
  observe(callback: (user: IdentityUser | null) => void): () => void;
  signInWithEmailAndPassword(email: string, password: string): Promise<IdentityUser>;
  signInWithGoogle(): Promise<IdentityUser>;
  resetPassword(email: string): Promise<void>;
  signOut(): Promise<void>;
}
export const IDENTITY_ADAPTER = new InjectionToken<IdentityAdapter>(
  'Firebase Web identity adapter',
);
