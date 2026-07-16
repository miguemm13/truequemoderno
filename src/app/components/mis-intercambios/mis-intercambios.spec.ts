import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MisIntercambios } from './mis-intercambios';

describe('MisIntercambios', () => {
  let component: MisIntercambios;
  let fixture: ComponentFixture<MisIntercambios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisIntercambios]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MisIntercambios);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
