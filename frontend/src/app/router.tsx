import { Navigate, Route, Routes } from 'react-router-dom';
import { BookingPage } from '../modules/booking/pages/BookingPage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/booking" replace />} />
      <Route path="/booking" element={<BookingPage />} />
      <Route path="*" element={<Navigate to="/booking" replace />} />
    </Routes>
  );
}

