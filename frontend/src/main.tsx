/**
 * Application entry point for the DPS Tracker Mini App.
 *
 * <p>Mounts the root {@link App} component into the {@code #root} DOM element
 * with React 19 {@code StrictMode} enabled for development-time checks.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.tsx';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
