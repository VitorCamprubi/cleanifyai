import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { loginGuard } from './login.guard';

describe('loginGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;
  let redirectTree: UrlTree;

  beforeEach(() => {
    redirectTree = { toString: () => '/dashboard' } as UrlTree;
    router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    router.createUrlTree.and.returnValue(redirectTree);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService }
      ]
    });
  });

  it('allows anonymous users to open the login screen', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => loginGuard({} as never, {} as never));

    expect(result).toBeTrue();
    expect(router.createUrlTree).not.toHaveBeenCalled();
  });

  it('redirects authenticated users away from the login screen', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => loginGuard({} as never, {} as never));

    expect(result).toBe(redirectTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
  });
});
