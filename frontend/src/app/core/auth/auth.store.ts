import { Injectable } from '@angular/core';
import { GemsBaseStore } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { AuthContext, AuthMe } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthStore extends GemsBaseStore {
  constructor() {
    super('api/auth');
  }

  getMe(): Observable<AuthMe> {
    return this.get<AuthMe>('me');
  }

  getContext(): Observable<AuthContext> {
    return this.get<AuthContext>('context');
  }
}
