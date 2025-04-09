import { AfterViewInit, Component } from '@angular/core';
import { StoreService } from '../service/store.service';
import { ProductDetails } from '../model/product';
import { RouterModule } from '@angular/router';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSort, faSortDown, faSortUp, faTrashRestore } from '@fortawesome/free-solid-svg-icons';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { CartProductComponent } from './cart-product/cart-product.component';
import { MatSliderModule } from '@angular/material/slider';

@Component({
  selector: 'app-product',
  imports: [RouterModule, NgbPagination, FormsModule, FaIconComponent, NgForOf, NgIf, CartProductComponent, NgClass, MatSliderModule],
  standalone: true,
  templateUrl: './product.component.html',
  styleUrl: './product.component.scss'
})
export class ProductComponent implements AfterViewInit {

  products: ProductDetails[] = [];
  page = 1;
  pageSize = 6;
  totalElements = 0;
  search = '';
  sort = 'id';
  direction = 'asc';
  selectedCategories: string[] = [];
  allCategories: string[] = [];

  faSort = faSort;
  faSortUp = faSortUp;
  faSortDown = faSortDown;

  minPrice = 0;
  maxPrice = 9999;
  maxPriceFromData: number = 9999;

  constructor(private storeService: StoreService) {}

  ngAfterViewInit(): void {
    this.getProducts();
  }

  getProducts(): void {
    this.storeService.getProducts(
      this.page - 1,
      this.pageSize,
      this.sort,
      this.direction,
      this.search,
      this.minPrice,
      this.maxPrice,
      this.selectedCategories
    ).subscribe(products => {
      this.products = products.products;
      this.allCategories = products.categories;
      if (this.maxPriceFromData == 9999) {
        this.maxPriceFromData = Math.floor(products.maxPrice + 1);
        this.maxPrice = this.maxPriceFromData;
      }
      this.totalElements = products.totalElements;
    });
  }

  onPriceChange(): void {
    if (this.minPrice > this.maxPrice) {
      const temp = this.minPrice;
      this.minPrice = this.maxPrice;
      this.maxPrice = temp;
    }
    this.page = 1;
    this.getProducts();
  }

  onPageChange(page: number): void {
    this.page = page;
    this.getProducts();
  }

  sortBy(column: string): void {
    if (this.sort === column) {
      this.direction = this.direction === 'asc' ? 'desc' : 'asc';
    } else {
      this.sort = column;
      this.direction = 'asc';
    }
    this.getProducts();
  }

  onSearch(): void {
    this.page = 1;
    this.getProducts();
  }

  clearCategoryFilters() {
    this.selectedCategories = [];
    this.getProducts();
  }

  filterByCategory(category: string): void {
    const index = this.selectedCategories.indexOf(category);

    if (index === -1) {
      this.selectedCategories.push(category);
    } else {
      this.selectedCategories.splice(index, 1);
    }
    this.getProducts();
  }

  protected readonly faTrashRestore = faTrashRestore;
}
