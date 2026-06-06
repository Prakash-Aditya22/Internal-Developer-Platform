# IDP Frontend

A modern React + Tailwind CSS frontend for the Internal Developer Platform.

## Getting Started

### Prerequisites
- Node.js 16+
- npm or yarn

### Installation

```bash
cd frontend
npm install
```

### Development

Start the development server (runs on http://localhost:5173):

```bash
npm run dev
```

The dev server will proxy API requests to `http://localhost:8080` automatically.

### Build for Production

```bash
npm run build
```

The production build will be in the `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

## Features

- **Authentication**: Login/Register with JWT tokens
- **Dashboard**: Overview of environments and deployments
- **Environments**: Create, manage, and control development environments
- **Deployments**: View deployment history and logs
- **Real-time Updates**: Live environment and deployment status

## Architecture

### Pages
- `Login` - User authentication
- `Register` - New user registration
- `Dashboard` - Overview and quick stats
- `Environments` - List and manage environments
- `Deployments` - View deployment history

### Components
- `Navigation` - Top navigation with user menu
- `ProtectedRoute` - Route guard for authenticated pages
- `EnvironmentCard` - Individual environment display
- `CreateEnvironmentModal` - Form for creating new environments

### Services
- `api.js` - Axios configuration with JWT handling and API endpoints

### Context
- `AuthContext` - Global authentication state and user management

## API Integration

The frontend communicates with the Spring Boot backend API at `http://localhost:8080/api/v1`.

### Key Endpoints Used
- POST `/auth/login`
- POST `/auth/register`
- GET `/auth/me`
- GET/POST `/environments`
- GET/POST `/deployments`

## Environment Variables

Create a `.env` file if needed (optional, dev server uses proxy):

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Troubleshooting

### API Connection Issues
- Ensure the Spring Boot backend is running on `localhost:8080`
- Check that CORS is properly configured in the backend
- Verify the proxy settings in `vite.config.js` match your backend URL

### Build Issues
- Clear `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Clear Vite cache: `rm -rf .vite`

## Technologies Used

- **React 18** - UI framework
- **Vite** - Build tool
- **Tailwind CSS v4** - Styling
- **React Router v6** - Routing
- **Axios** - HTTP client
- **PostCSS** - CSS processing
