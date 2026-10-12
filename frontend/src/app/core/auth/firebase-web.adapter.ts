import { Injectable } from '@angular/core';
import { initializeApp, getApps } from 'firebase/app';
import {
  Auth,
  connectAuthEmulator,
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from 'firebase/auth';
import { environment } from '../../../environments/environment';
import { IdentityAdapter, IdentityUser } from './identity.adapter';

@Injectable({ providedIn: 'root' })
export class FirebaseWebAdapter implements IdentityAdapter {
  private auth: Auth | undefined;

  observe(callback: (user: IdentityUser | null) => void): () => void {
    if (!environment.firebase.apiKey || !environment.firebase.projectId) {
      callback(null);
      return () => {};
    }
    return onAuthStateChanged(this.instance(), callback, () => callback(null));
  }

  async signInWithEmailAndPassword(email: string, password: string): Promise<IdentityUser> {
    return (await signInWithEmailAndPassword(this.instance(), email, password)).user;
  }

  async signInWithGoogle(): Promise<IdentityUser> {
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: 'select_account' });
    return (await signInWithPopup(this.instance(), provider)).user;
  }

  resetPassword(email: string): Promise<void> {
    return sendPasswordResetEmail(this.instance(), email);
  }

  signOut(): Promise<void> {
    return this.auth ? signOut(this.auth) : Promise.resolve();
  }

  private instance(): Auth {
    if (this.auth) return this.auth;
    if (!environment.firebase.apiKey || !environment.firebase.projectId)
      throw new Error('identity-unavailable');
    if (
      environment.authEmulatorUrl &&
      (environment.production ||
        !/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(environment.authEmulatorUrl) ||
        !environment.firebase.projectId.startsWith('demo-'))
    )
      throw new Error('invalid-emulator-config');
    const app =
      getApps().find((value) => value.name === 'sample') ??
      initializeApp(environment.firebase, 'sample');
    const auth = getAuth(app);
    if (environment.authEmulatorUrl) {
      connectAuthEmulator(auth, environment.authEmulatorUrl, { disableWarnings: true });
    }
    this.auth = auth;
    return this.auth;
  }
}
