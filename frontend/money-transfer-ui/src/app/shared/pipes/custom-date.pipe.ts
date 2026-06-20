import { Pipe, PipeTransform } from '@angular/core';
import { formatDate } from '../../core/utils/date.util';

@Pipe({
  name: 'customDate',
  standalone: true
})
export class CustomDatePipe implements PipeTransform {
  transform(value: string | Date | number | null | undefined, showTime: boolean = false, showTimezone: boolean = false): string {
    if (!value) return '';
    return formatDate(value, showTime, showTimezone);
  }
}
