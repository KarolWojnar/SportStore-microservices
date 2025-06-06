import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserDetails, UserStatus } from '../model/user-dto';
import { CategoryNew, ProductInfo, ProductsResponse } from '../model/product';
import { OrderBaseInfo } from '../model/order';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private httpClient: HttpClient) { }

  getAllUsers(
    page: number,
    search: string = '',
    role: "ROLE_ADMIN" | "ROLE_CUSTOMER" | null = null,
    enabled: boolean | null = null
  ): Observable<UserDetails[]> {
    let params = new HttpParams()
      .set('page', page.toString());

    if (search) params = params.set('search', search);
    if (role) params = params.set('role', role);
    if (enabled !== null) params = params.set('enabled', enabled.toString());

    return this.httpClient.get<UserDetails[]>(`${this.apiUrl}/auth/admin`, { params });
  }

  setActivationUser(userId: string, status: UserStatus) {
    return this.httpClient.patch(`${this.apiUrl}/auth/admin/${userId}`, status);
  }

  setAdmin(id: string) {
    return this.httpClient.patch(`${this.apiUrl}/auth/admin/${id}/role`, {});
  }

  getAllProducts(page: number, search?: string, category: string[] = []): Observable<ProductsResponse> {
    let params = new HttpParams().set('page', page.toString());

    if (search) {
      params = params.set('search', search);
    }

    if (category.length > 0) {
      params = params.set('categories', category.join(', '));
    }

    params = params.set('size', 10);

    return this.httpClient.get<ProductsResponse>(`${this.apiUrl}/products`, { params });
  }

  updateProduct(productId: string, editedProduct: ProductInfo): Observable<ProductInfo> {
    return this.httpClient.patch<ProductInfo>(`${this.apiUrl}/products/admin/${productId}`, editedProduct);
  }

  changeAvailability(productId: string, newAvailability: boolean) {
    return this.httpClient.patch(`${this.apiUrl}/products/admin/${productId}/available`, newAvailability);
  }

  addProduct(formData: FormData) {
    return this.httpClient.post(`${this.apiUrl}/products/admin`, formData);
  }

  addCategory(category: { name: string }): Observable<CategoryNew> {
    return this.httpClient.post<CategoryNew>(`${this.apiUrl}/products/admin/categories`, category);
  }

  getOrders(page: number = 0, selectedStatus: string | null): Observable<OrderBaseInfo[]>{
    let params = new HttpParams().set('page', page);

    if (selectedStatus) {
      params = params.set('status', selectedStatus);
    }
    return this.httpClient.get<OrderBaseInfo[]>(`${this.apiUrl}/orders/admin`, { params });
  }

  cancelOrder(orderId: string) {
    return this.httpClient.patch(`${this.apiUrl}/orders/cancel/${orderId}`, null);
  }
}
