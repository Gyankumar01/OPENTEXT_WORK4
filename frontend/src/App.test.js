import { render, screen } from '@testing-library/react';
import App from './App';

// Define a test case using Jest
test('renders learn react link', () => {
  // Render the App component within a testing environment
  render(<App />);

  // Use screen utility to find an element containing 'learn react' text (case insensitive)
  const linkElement = screen.getByText(/learn react/i);

  // Expectation: Assert that the linkElement is present in the document
  expect(linkElement).toBeInTheDocument();
});
