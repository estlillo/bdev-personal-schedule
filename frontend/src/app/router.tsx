import { Navigate, Route, Routes } from 'react-router-dom';
import { BookingPage } from '../modules/booking/pages/BookingPage';
import { ProfessionalAvailabilityPage } from '../modules/schedule/pages/ProfessionalAvailabilityPage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/booking" replace />} />
      <Route path="/booking" element={<BookingPage />} />
      <Route path="/availability" element={<ProfessionalAvailabilityPage />} />
      <Route path="*" element={<Navigate to="/booking" replace />} />
    </Routes>
  );
}

