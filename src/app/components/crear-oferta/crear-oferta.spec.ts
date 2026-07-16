import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CrearOferta } from './crear-oferta';

describe('CrearOferta', () => {
  let component: CrearOferta;
  let fixture: ComponentFixture<CrearOferta>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CrearOferta]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CrearOferta);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
