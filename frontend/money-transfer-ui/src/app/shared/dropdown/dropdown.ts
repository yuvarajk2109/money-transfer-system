import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

export interface DropdownOption {
  label: string;
  value: any;
  disabled?: boolean;
}

@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dropdown.html',
  styleUrl: './dropdown.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Dropdown),
      multi: true
    }
  ]
})
export class Dropdown implements ControlValueAccessor {
  @Input() options: DropdownOption[] = [];
  @Input() placeholder?: string;
  @Input() placeholderValue: any = '';
  @Input() placeholderDisabled = false;
  @Input() isTableFilter = false;
  @Input() isAccountFilter = false;
  @Input() name = '';
  @Input() id = '';

  @Output() selectionChange = new EventEmitter<any>();

  value: any;
  disabled = false;

  onChange = (value: any) => { };
  onTouched = () => { };

  writeValue(val: any): void {
    this.value = val;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onValueChange(val: any) {
    this.value = val;
    this.onChange(val);
    this.selectionChange.emit(val);
  }
}
