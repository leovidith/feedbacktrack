import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/admin/departments`;

  getAll(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.base}/all`);
  }

  add(department: { departmentName: string }): Observable<Department> {
    return this.http.post<Department>(`${this.base}/add`, department);
  }
}
