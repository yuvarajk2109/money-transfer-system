export function formatDate(
  dateValue: string | Date | number,
  showTime: boolean = false,
  showTimezone: boolean = false
): string {
  if (!dateValue) return '';
  const date = new Date(dateValue);
  if (isNaN(date.getTime())) return String(dateValue);

  const day = String(date.getDate()).padStart(2, '0');
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
  const month = monthNames[date.getMonth()];
  const year = date.getFullYear();

  let formatted = `${day} ${month} ${year}`;

  if (showTime) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    formatted += ` ${hours}:${minutes}:${seconds}`;
  }

  if (showTimezone) {
    const offset = -date.getTimezoneOffset();
    const sign = offset >= 0 ? '+' : '-';
    const absOffset = Math.abs(offset);
    const tzHours = Math.floor(absOffset / 60);
    const tzMinutes = String(absOffset % 60).padStart(2, '0');
    formatted += ` ${sign}${tzHours}:${tzMinutes}`;
  }

  return formatted;
}
