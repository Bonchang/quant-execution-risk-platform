import type { ReactNode } from 'react';

interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
}

export function DataTable<T>({
  columns,
  rows,
  emptyMessage = '데이터 없음',
}: {
  columns: Column<T>[];
  rows: T[];
  emptyMessage?: string;
}) {
  return (
    <div className="table-shell">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.header}>{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row, index) => (
              <tr key={index}>
                {columns.map((column) => (
                  <td key={column.header}>{column.render(row)}</td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length}>{emptyMessage}</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
