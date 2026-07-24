import { Outlet } from 'react-router-dom'; import { Footer } from '../components/layout/Footer'; import { Navbar } from '../components/layout/Navbar';
export function MainLayout(){return <><Navbar/><main><Outlet/></main><Footer/></>}
