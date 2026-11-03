import { Navigate, Route, Routes } from 'react-router-dom';
import { BookingPage } from '../modules/booking/pages/BookingPage';
import { ReservationsCalendarPage } from '../modules/booking/pages/ReservationsCalendarPage';
import { InstitutionalSchedulePage } from '../modules/institutional/pages/InstitutionalSchedulePage';
import { ProfessionalsCrudPage } from '../modules/professionals/pages/ProfessionalsCrudPage';
import { ProfessionalAvailabilityPage } from '../modules/schedule/pages/ProfessionalAvailabilityPage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/booking" replace />} />
      <Route path="/booking" element={<BookingPage />} />
      <Route path="/calendar" element={<ReservationsCalendarPage />} />
      <Route path="/institutional-schedule" element={<InstitutionalSchedulePage />} />
      <Route path="/professionals" element={<ProfessionalsCrudPage />} />
      <Route path="/availability" element={<ProfessionalAvailabilityPage />} />
      <Route path="*" element={<Navigate to="/booking" replace />} />
    </Routes>
  );
}

