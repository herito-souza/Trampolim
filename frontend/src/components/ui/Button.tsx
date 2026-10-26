import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost';
}

export function Button({ variant = 'primary', className = '', children, ...props }: ButtonProps) {
  const baseClasses = "font-sans font-extrabold text-[15px] px-[26px] py-[14px] rounded-full cursor-pointer transition-transform duration-150 ease-out hover:-translate-y-[2px]";
  const variants = {
    primary: "bg-ink text-brand-white border-none",
    ghost: "bg-transparent text-ink border-2 border-ink border-solid"
  };

  return (
    <button className={`${baseClasses} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
}
