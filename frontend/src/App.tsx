import { BrowserRouter } from 'react-router-dom';
import { DemoProvider } from './context/DemoContext';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { AppRoutes } from './routes/AppRoutes';

export function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <DemoProvider>
          <AuthProvider>
            <AppRoutes />
          </AuthProvider>
        </DemoProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
