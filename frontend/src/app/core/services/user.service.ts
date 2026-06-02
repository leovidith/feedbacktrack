import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChangePasswordRequest, CreateUserRequest, Role, User } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/admin/users`;

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/all`);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.base}/${id}`);
  }

  getByRole(role: Role): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/role/${role}`);
  }

  getTeam(managerId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}/manager/${managerId}/team`);
  }

  add(req: CreateUserRequest): Observable<User> {
    return this.http.post<User>(`${this.base}/add`, req);
  }

  updateRole(email: string, role: Role): Observable<User> {
    const params = new URLSearchParams({ role });
    return this.http.put<User>(`${this.base}/${email}/role?${params.toString()}`, {});
  }

  activate(email: string): Observable<User> {
    const params = new URLSearchParams({ email });
    return this.http.put<User>(`${this.base}/activate?${params.toString()}`, {});
  }

  deactivate(email: string): Observable<User> {
    const params = new URLSearchParams({ email });
    return this.http.put<User>(`${this.base}/deactivate?${params.toString()}`, {});
  }

  changePassword(req: ChangePasswordRequest): Observable<string> {
    return this.http.put(`${this.base}/profile/change-password`, req, { responseType: 'text' });
  }
}
